package com.raizesdonordeste.application.services;

import com.raizesdonordeste.api.dtos.request.PagamentoRequestDto;
import com.raizesdonordeste.api.dtos.response.PagamentoResponseDto;
import com.raizesdonordeste.domain.entities.ItemPedido;
import com.raizesdonordeste.domain.entities.Pagamento;
import com.raizesdonordeste.domain.entities.Pedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.enums.StatusTransacao;
import com.raizesdonordeste.domain.exceptions.BusinessConflictException;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import com.raizesdonordeste.infrastructure.gateway.GatewayPagamentoMock;
import com.raizesdonordeste.infrastructure.gateway.ProcessamentoPagamentoResult;
import com.raizesdonordeste.infrastructure.repositories.EstoqueRepository;
import com.raizesdonordeste.infrastructure.repositories.PagamentoRepository;
import com.raizesdonordeste.infrastructure.repositories.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepository pedidoRepository;
    private final EstoqueRepository estoqueRepository;
    private final GatewayPagamentoMock gatewayPagamentoMock;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PagamentoResponseDto processarPagamento(PagamentoRequestDto request) {
        log.info("Iniciando processamento de pagamento. Pedido ID: {}, Método: {}, Chave idempotência: {}",
                request.getPedidoId(), request.getMetodoPagamento(), request.getChaveIdempotencia());

        // 1. Validação de Idempotência
        if (pagamentoRepository.existsByChaveIdempotencia(request.getChaveIdempotencia())) {
            throw new BusinessConflictException(
                    "CHAVE_IDEMPOTENCIA_DUPLICADA",
                    String.format("A chave de idempotência '%s' já foi processada anteriormente.", request.getChaveIdempotencia())
            );
        }

        // 2. Busca do Pedido
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + request.getPedidoId()));

        // 3. Validação do Status do Pedido
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new BusinessConflictException(
                    "PAGAMENTO_JA_PROCESSADO",
                    String.format("O pedido ID %d não está aguardando pagamento. Status atual: %s.",
                            pedido.getId(), pedido.getStatus())
            );
        }

        // 4. Verificação de pagamento pré-existente
        if (pagamentoRepository.existsByPedidoId(pedido.getId())) {
            throw new BusinessConflictException(
                    "PAGAMENTO_JA_EXISTENTE",
                    "Já existe um registro de pagamento associado a este pedido."
            );
        }

        // 5. Chamada desacoplada ao Gateway Mock
        ProcessamentoPagamentoResult resultado = gatewayPagamentoMock.processar(
                pedido,
                request.getMetodoPagamento(),
                request.getChaveIdempotencia(),
                request.getSimularStatus()
        );

        // 6. Persistência do Pagamento
        Pagamento pagamento = Pagamento.builder()
                .pedido(pedido)
                .metodoPagamento(request.getMetodoPagamento())
                .statusTransacao(resultado.getStatusTransacao())
                .chaveIdempotencia(request.getChaveIdempotencia())
                .payloadResposta(resultado.getPayloadResposta())
                .build();

        pagamento = pagamentoRepository.save(pagamento);
        pedido.associarPagamento(pagamento);

        // 7. Atualização do Pedido e Estoque conforme retorno
        if (resultado.getStatusTransacao() == StatusTransacao.APROVADO) {
            pedido.setStatus(StatusPedido.PREPARACAO);
            pedidoRepository.save(pedido);

            auditoriaService.registrarLog(
                    "PAGAMENTO_APROVADO",
                    "PAGAMENTO",
                    pagamento.getId(),
                    String.format("Pagamento de R$ %s APROVADO para o Pedido ID %d via %s.",
                            pedido.getValorTotal(), pedido.getId(), request.getMetodoPagamento())
            );

            auditoriaService.registrarLog(
                    "ATUALIZACAO_STATUS_PEDIDO",
                    "PEDIDO",
                    pedido.getId(),
                    String.format("Pedido ID %d avançado para PREPARACAO após confirmação de pagamento.", pedido.getId())
            );
            log.info("Pagamento aprovado. Pedido ID {} avançado para PREPARACAO.", pedido.getId());
        } else {
            // Pagamento RECUSADO: Cancela pedido e estorna estoque
            pedido.setStatus(StatusPedido.CANCELADO);

            if (pedido.getItens() != null) {
                for (ItemPedido item : pedido.getItens()) {
                    estoqueRepository.findByUnidadeIdAndProdutoId(pedido.getUnidade().getId(), item.getProduto().getId())
                            .ifPresent(estoque -> {
                                estoque.adicionarEstoque(item.getQuantidade());
                                estoqueRepository.save(estoque);
                                log.info("Estoque estornado para o produto ID {} na unidade ID {}. Quantidade estornada: {}",
                                        item.getProduto().getId(), pedido.getUnidade().getId(), item.getQuantidade());
                            });
                }
            }

            pedidoRepository.save(pedido);

            auditoriaService.registrarLog(
                    "PAGAMENTO_RECUSADO",
                    "PAGAMENTO",
                    pagamento.getId(),
                    String.format("Pagamento RECUSADO para o Pedido ID %d via %s. Pedido cancelado e estoque estornado.",
                            pedido.getId(), request.getMetodoPagamento())
            );

            auditoriaService.registrarLog(
                    "CANCELAMENTO_PEDIDO",
                    "PEDIDO",
                    pedido.getId(),
                    "Pedido cancelado automaticamente devido à recusa do pagamento. Saldo de estoque estornado."
            );
            log.warn("Pagamento recusado. Pedido ID {} cancelado e estoque estornado.", pedido.getId());
        }

        return PagamentoResponseDto.fromEntity(pagamento);
    }

    @Transactional(readOnly = true)
    public PagamentoResponseDto buscarPorId(Long id) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o ID: " + id));
        return PagamentoResponseDto.fromEntity(pagamento);
    }

    @Transactional(readOnly = true)
    public PagamentoResponseDto buscarPorPedidoId(Long pedidoId) {
        Pagamento pagamento = pagamentoRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum pagamento encontrado para o Pedido ID: " + pedidoId));
        return PagamentoResponseDto.fromEntity(pagamento);
    }
}
