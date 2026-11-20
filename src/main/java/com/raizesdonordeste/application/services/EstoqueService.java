package com.raizesdonordeste.application.services;

import com.raizesdonordeste.api.dtos.request.EstoqueUpdateRequestDto;
import com.raizesdonordeste.api.dtos.response.EstoqueResponseDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.domain.entities.Estoque;
import com.raizesdonordeste.domain.entities.Produto;
import com.raizesdonordeste.domain.entities.Unidade;
import com.raizesdonordeste.domain.enums.TipoMovimentacaoEstoque;
import com.raizesdonordeste.domain.exceptions.BusinessValidationException;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import com.raizesdonordeste.infrastructure.repositories.EstoqueRepository;
import com.raizesdonordeste.infrastructure.repositories.ProdutoRepository;
import com.raizesdonordeste.infrastructure.repositories.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public PaginatedResponseDto<EstoqueResponseDto> consultarEstoquePorUnidade(Long unidadeId, int page, int limit) {
        log.info("Consultando saldo de estoque da unidade ID: {}. Página: {}, Limite: {}", unidadeId, page, limit);

        if (!unidadeRepository.existsById(unidadeId)) {
            throw new ResourceNotFoundException("Unidade não encontrada com o ID: " + unidadeId);
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by("produto.id").ascending());
        Page<Estoque> estoquePage = estoqueRepository.findByUnidadeId(unidadeId, pageable);

        return PaginatedResponseDto.fromPage(estoquePage, EstoqueResponseDto::fromEntity);
    }

    @Transactional
    public EstoqueResponseDto atualizarEstoque(EstoqueUpdateRequestDto request) {
        log.info("Processando atualização de estoque: Unidade {}, Produto {}, Tipo {}, Qtd {}",
                request.getUnidadeId(), request.getProdutoId(), request.getTipoMovimentacao(), request.getQuantidade());

        Unidade unidade = unidadeRepository.findById(request.getUnidadeId())
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada com o ID: " + request.getUnidadeId()));

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + request.getProdutoId()));

        if (request.getTipoMovimentacao() != TipoMovimentacaoEstoque.AJUSTE && request.getQuantidade() <= 0) {
            throw new BusinessValidationException(
                    "A quantidade informada para movimentações de ENTRADA ou SAIDA deve ser estritamente maior que zero.",
                    Collections.singletonList("quantidade: deve ser maior que zero para operações de " + request.getTipoMovimentacao())
            );
        }

        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId())
                .orElseGet(() -> Estoque.builder()
                        .unidade(unidade)
                        .produto(produto)
                        .quantidadeDisponivel(0)
                        .build());

        int saldoAnterior = estoque.getQuantidadeDisponivel();

        switch (request.getTipoMovimentacao()) {
            case ENTRADA -> estoque.adicionarEstoque(request.getQuantidade());
            case SAIDA -> estoque.subtrairEstoque(request.getQuantidade());
            case AJUSTE -> estoque.definirEstoque(request.getQuantidade());
        }

        Estoque salvo = estoqueRepository.save(estoque);
        int novoSaldo = salvo.getQuantidadeDisponivel();

        String motivo = (request.getMotivo() != null && !request.getMotivo().isBlank())
                ? request.getMotivo().trim()
                : "Sem motivo especificado";

        String detalhesAuditoria = String.format(
                "Movimentação [%s] na unidade '%s' (ID %d) para o produto '%s' (ID %d). Quantidade movimentada: %d. Saldo anterior: %d. Novo saldo: %d. Motivo: %s.",
                request.getTipoMovimentacao(), unidade.getNome(), unidade.getId(),
                produto.getNome(), produto.getId(), request.getQuantidade(),
                saldoAnterior, novoSaldo, motivo
        );

        auditoriaService.registrarLog(
                "MOVIMENTACAO_ESTOQUE_" + request.getTipoMovimentacao().name(),
                "ESTOQUE",
                salvo.getId(),
                detalhesAuditoria
        );

        log.info("Estoque atualizado com sucesso. Registro ID: {}, Saldo anterior: {}, Novo saldo: {}",
                salvo.getId(), saldoAnterior, novoSaldo);

        return EstoqueResponseDto.fromEntity(salvo);
    }
}
