package com.raizesdonordeste.application.services;

import com.raizesdonordeste.api.dtos.request.ItemPedidoRequestDto;
import com.raizesdonordeste.api.dtos.request.PedidoCreateRequestDto;
import com.raizesdonordeste.api.dtos.request.PedidoStatusUpdateRequestDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.api.dtos.response.PedidoResponseDto;
import com.raizesdonordeste.domain.entities.*;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.Perfil;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.exceptions.BusinessConflictException;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import com.raizesdonordeste.infrastructure.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PedidoResponseDto criarPedido(PedidoCreateRequestDto request) {
        log.info("Iniciando criação de pedido. Unidade: {}, Canal: {}, Itens: {}",
                request.getUnidadeId(), request.getCanalPedido(), request.getItens().size());

        // 1. Validação da Unidade
        Unidade unidade = unidadeRepository.findById(request.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada com o ID: " + request.getUnidadeId()));

        if (!Boolean.TRUE.equals(unidade.getAtiva())) {
            throw new BusinessConflictException("UNIDADE_INATIVA", "A unidade selecionada está inativa para novos pedidos.");
        }

        // 2. Resolução do Cliente (Autenticado, Explicitamente Informado ou Anônimo)
        Usuario cliente = resolverCliente(request.getClienteId());

        // 3. Agrupamento de itens por produto para evitar duplicidade na mesma requisição
        Map<Long, Integer> itensAgrupados = new LinkedHashMap<>();
        for (ItemPedidoRequestDto itemDto : request.getItens()) {
            itensAgrupados.merge(itemDto.getProdutoId(), itemDto.getQuantidade(), Integer::sum);
        }

        // 4. Validação de Produtos, Estoque e Cálculo de Valor
        BigDecimal valorTotal = BigDecimal.ZERO;
        List<ItemPedido> itens = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : itensAgrupados.entrySet()) {
            Long produtoId = entry.getKey();
            Integer quantidade = entry.getValue();

            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + produtoId));

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new BusinessConflictException(
                        "PRODUTO_INATIVO",
                        String.format("O produto '%s' (ID %d) está inativo no catálogo.", produto.getNome(), produto.getId())
                );
            }

            Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId())
                    .orElseThrow(() -> new BusinessConflictException(
                            "ESTOQUE_INSUFICIENTE",
                            String.format("Saldo insuficiente em estoque para o produto '%s' na unidade ID %d. Solicitado: %d, Disponível: 0.",
                                    produto.getNome(), unidade.getId(), quantidade)
                    ));

            // Baixa atômica no estoque
            estoque.subtrairEstoque(quantidade);
            estoqueRepository.save(estoque);

            BigDecimal precoUnitarioGarantido = produto.getPrecoBase();
            BigDecimal subtotal = precoUnitarioGarantido.multiply(BigDecimal.valueOf(quantidade));
            valorTotal = valorTotal.add(subtotal);

            ItemPedido itemPedido = ItemPedido.builder()
                    .produto(produto)
                    .quantidade(quantidade)
                    .precoUnitarioGarantido(precoUnitarioGarantido)
                    .build();

            itens.add(itemPedido);
        }

        // 5. Construção e Persistência do Pedido
        Pedido pedido = Pedido.builder()
                .unidade(unidade)
                .cliente(cliente)
                .canalPedido(request.getCanalPedido())
                .status(StatusPedido.AGUARDANDO_PAGAMENTO)
                .valorTotal(valorTotal)
                .build();

        for (ItemPedido item : itens) {
            pedido.adicionarItem(item);
        }

        Pedido salvo = pedidoRepository.save(pedido);

        // 6. Auditoria de Criação
        String detalhesAuditoria = String.format(
                "Pedido ID %d criado com sucesso via canal '%s' na unidade '%s' (ID %d). Total de itens: %d. Valor total: R$ %s.",
                salvo.getId(), salvo.getCanalPedido(), unidade.getNome(), unidade.getId(),
                salvo.getItens().size(), salvo.getValorTotal()
        );

        auditoriaService.registrarLog("CRIACAO_PEDIDO", "PEDIDO", salvo.getId(), detalhesAuditoria);
        log.info("Pedido ID {} criado com sucesso. Total: R$ {}", salvo.getId(), salvo.getValorTotal());

        return PedidoResponseDto.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public PedidoResponseDto buscarPorId(Long id) {
        log.info("Consultando detalhes do pedido ID: {}", id);

        Pedido pedido = pedidoRepository.findByIdWithDetails(id)
                .orElseGet(() -> pedidoRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id)));

        verificarVisibilidadePedido(pedido);

        return PedidoResponseDto.fromEntity(pedido);
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<PedidoResponseDto> listarPedidos(
            CanalPedido canalPedido,
            StatusPedido status,
            Long unidadeId,
            int page,
            int limit
    ) {
        log.info("Listando pedidos. Filtros - Canal: {}, Status: {}, Unidade: {}, Página: {}, Limite: {}",
                canalPedido, status, unidadeId, page, limit);

        Long clienteIdFiltro = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Optional<Usuario> usuarioLogadoOpt = usuarioRepository.findByEmail(authentication.getName());
            if (usuarioLogadoOpt.isPresent() && usuarioLogadoOpt.get().getPerfil() == Perfil.CLIENTE) {
                // Cliente só pode listar os próprios pedidos
                clienteIdFiltro = usuarioLogadoOpt.get().getId();
            }
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Pedido> pedidosPage = pedidoRepository.findWithFilters(canalPedido, status, unidadeId, clienteIdFiltro, pageable);

        return PaginatedResponseDto.fromPage(pedidosPage, PedidoResponseDto::fromEntity);
    }

    @Transactional
    public PedidoResponseDto atualizarStatus(Long id, PedidoStatusUpdateRequestDto request) {
        log.info("Processando atualização de status do pedido ID: {}. Novo status: {}", id, request.getNovoStatus());

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado com o ID: " + id));

        StatusPedido statusAtual = pedido.getStatus();
        StatusPedido novoStatus = request.getNovoStatus();

        validarTransicaoStatus(statusAtual, novoStatus);

        // Se o pedido for cancelado, estorna o estoque reservado
        if (novoStatus == StatusPedido.CANCELADO) {
            if (pedido.getItens() != null) {
                for (ItemPedido item : pedido.getItens()) {
                    estoqueRepository.findByUnidadeIdAndProdutoId(pedido.getUnidade().getId(), item.getProduto().getId())
                            .ifPresent(estoque -> {
                                estoque.adicionarEstoque(item.getQuantidade());
                                estoqueRepository.save(estoque);
                                log.info("Estoque estornado ao cancelar pedido ID {}. Produto ID: {}, Quantidade: {}",
                                        pedido.getId(), item.getProduto().getId(), item.getQuantidade());
                            });
                }
            }
        }

        // Se o pedido for ENTREGUE, acumula pontos de fidelidade se houver consentimento LGPD
        if (novoStatus == StatusPedido.ENTREGUE && pedido.getCliente() != null) {
            Usuario cliente = pedido.getCliente();
            if (Boolean.TRUE.equals(cliente.getConsentimentoLgpd())) {
                int pontosGanhos = Math.max(1, pedido.getValorTotal().intValue());
                cliente.setPontosFidelidade(cliente.getPontosFidelidade() + pontosGanhos);
                usuarioRepository.save(cliente);

                auditoriaService.registrarLog(
                        "ACUMULO_FIDELIDADE",
                        "USUARIO",
                        cliente.getId(),
                        String.format("Creditados %d pontos de fidelidade para o cliente '%s' (ID %d) pela conclusão do pedido ID %d.",
                                pontosGanhos, cliente.getNome(), cliente.getId(), pedido.getId())
                );
                log.info("Pontos de fidelidade creditados: {} pontos para o cliente ID {}", pontosGanhos, cliente.getId());
            }
        }

        pedido.setStatus(novoStatus);
        Pedido salvo = pedidoRepository.save(pedido);

        String motivo = (request.getMotivo() != null && !request.getMotivo().isBlank())
                ? request.getMotivo().trim()
                : "Sem motivo especificado";

        String detalhesAuditoria = String.format(
                "Status do pedido ID %d alterado de '%s' para '%s'. Motivo: %s.",
                salvo.getId(), statusAtual, novoStatus, motivo
        );

        auditoriaService.registrarLog("ATUALIZACAO_STATUS_PEDIDO", "PEDIDO", salvo.getId(), detalhesAuditoria);

        log.info("Status do pedido ID {} alterado com sucesso de {} para {}.", salvo.getId(), statusAtual, novoStatus);

        return PedidoResponseDto.fromEntity(salvo);
    }

    private void validarTransicaoStatus(StatusPedido atual, StatusPedido novo) {
        if (atual == StatusPedido.ENTREGUE || atual == StatusPedido.CANCELADO) {
            throw new BusinessConflictException(
                    "TRANSICAO_STATUS_INVALIDA",
                    String.format("Não é possível alterar o status de um pedido que já se encontra '%s'.", atual)
            );
        }

        if (atual == novo) {
            return;
        }

        boolean transicaoValida = false;

        switch (atual) {
            case AGUARDANDO_PAGAMENTO -> transicaoValida = (novo == StatusPedido.PREPARACAO || novo == StatusPedido.CANCELADO);
            case PREPARACAO -> transicaoValida = (novo == StatusPedido.PRONTO || novo == StatusPedido.CANCELADO);
            case PRONTO -> transicaoValida = (novo == StatusPedido.ENTREGUE || novo == StatusPedido.CANCELADO);
            default -> transicaoValida = false;
        }

        if (!transicaoValida) {
            throw new BusinessConflictException(
                    "TRANSICAO_STATUS_INVALIDA",
                    String.format("Transição de status inválida da esteira de '%s' para '%s'.", atual, novo)
            );
        }
    }

    private void verificarVisibilidadePedido(Pedido pedido) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
        if (usuarioOpt.isPresent() && usuarioOpt.get().getPerfil() == Perfil.CLIENTE) {
            if (pedido.getCliente() == null || !pedido.getCliente().getId().equals(usuarioOpt.get().getId())) {
                throw new AccessDeniedException("Você não possui permissão para visualizar pedidos de outros clientes.");
            }
        }
    }

    private Usuario resolverCliente(Long clienteIdParam) {
        if (clienteIdParam != null) {
            return usuarioRepository.findById(clienteIdParam)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com o ID: " + clienteIdParam));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(authentication.getName());
            if (usuarioOpt.isPresent() && usuarioOpt.get().getPerfil() == Perfil.CLIENTE) {
                return usuarioOpt.get();
            }
        }

        return null;
    }
}
