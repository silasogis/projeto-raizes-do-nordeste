package com.raizesdonordeste.application.services;

import com.raizesdonordeste.api.dtos.request.ProdutoCreateRequestDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.api.dtos.response.ProdutoResponseDto;
import com.raizesdonordeste.domain.entities.Estoque;
import com.raizesdonordeste.domain.entities.Produto;
import com.raizesdonordeste.domain.entities.Unidade;
import com.raizesdonordeste.domain.exceptions.BusinessConflictException;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;
    private final EstoqueRepository estoqueRepository;
    private final AuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public PaginatedResponseDto<ProdutoResponseDto> listarProdutos(int page, int limit) {
        log.info("Listando produtos do catálogo. Página: {}, Limite: {}", page, limit);
        Pageable pageable = PageRequest.of(page, limit, Sort.by("id").ascending());
        Page<Produto> produtosPage = produtoRepository.findAllByAtivoTrue(pageable);
        return PaginatedResponseDto.fromPage(produtosPage, ProdutoResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDto buscarPorId(Long id) {
        log.info("Buscando produto por ID: {}", id);
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado com o ID: " + id));
        return ProdutoResponseDto.fromEntity(produto);
    }

    @Transactional
    public ProdutoResponseDto criarProduto(ProdutoCreateRequestDto request) {
        String trimmedName = request.getNome().trim();
        log.info("Cadastrando novo produto: {}", trimmedName);

        if (produtoRepository.existsByNomeIgnoreCase(trimmedName)) {
            throw new BusinessConflictException(
                    "PRODUTO_JA_CADASTRADO",
                    "Já existe um produto cadastrado com o nome: " + trimmedName
            );
        }

        Produto produto = Produto.builder()
                .nome(trimmedName)
                .descricao(request.getDescricao() != null ? request.getDescricao().trim() : null)
                .precoBase(request.getPrecoBase())
                .ativo(request.getAtivo() != null ? request.getAtivo() : true)
                .build();

        Produto salvo = produtoRepository.save(produto);
        log.info("Produto cadastrado com sucesso. ID: {}", salvo.getId());

        // Inicializar registro de estoque com saldo 0 para todas as unidades ativas da rede
        List<Unidade> unidadesAtivas = unidadeRepository.findAllByAtivaTrue();
        for (Unidade unidade : unidadesAtivas) {
            if (!estoqueRepository.existsByUnidadeIdAndProdutoId(unidade.getId(), salvo.getId())) {
                Estoque estoqueInicial = Estoque.builder()
                        .unidade(unidade)
                        .produto(salvo)
                        .quantidadeDisponivel(0)
                        .build();
                estoqueRepository.save(estoqueInicial);
            }
        }

        auditoriaService.registrarLog(
                "CRIACAO_PRODUTO",
                "PRODUTO",
                salvo.getId(),
                String.format("Produto '%s' criado com preço base R$ %.2f. Status ativo: %s.",
                        salvo.getNome(), salvo.getPrecoBase(), salvo.getAtivo())
        );

        return ProdutoResponseDto.fromEntity(salvo);
    }
}
