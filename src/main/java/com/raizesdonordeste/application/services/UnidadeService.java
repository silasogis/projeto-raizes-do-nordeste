package com.raizesdonordeste.application.services;

import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.api.dtos.response.ProdutoCardapioResponseDto;
import com.raizesdonordeste.api.dtos.response.UnidadeResponseDto;
import com.raizesdonordeste.domain.entities.Estoque;
import com.raizesdonordeste.domain.entities.Unidade;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import com.raizesdonordeste.infrastructure.repositories.EstoqueRepository;
import com.raizesdonordeste.infrastructure.repositories.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final EstoqueRepository estoqueRepository;

    @Transactional(readOnly = true)
    public PaginatedResponseDto<UnidadeResponseDto> listarUnidades(int page, int limit) {
        log.info("Listando unidades da rede. Página: {}, Limite: {}", page, limit);
        Pageable pageable = PageRequest.of(page, limit, Sort.by("id").ascending());
        Page<Unidade> unidadesPage = unidadeRepository.findAllByAtivaTrue(pageable);
        return PaginatedResponseDto.fromPage(unidadesPage, UnidadeResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UnidadeResponseDto buscarPorId(Long id) {
        log.info("Buscando unidade por ID: {}", id);
        Unidade unidade = unidadeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada com o ID: " + id));
        return UnidadeResponseDto.fromEntity(unidade);
    }

    @Transactional(readOnly = true)
    public PaginatedResponseDto<ProdutoCardapioResponseDto> listarCardapioUnidade(Long unidadeId, int page, int limit) {
        log.info("Listando cardápio da unidade ID: {}. Página: {}, Limite: {}", unidadeId, page, limit);

        if (!unidadeRepository.existsById(unidadeId)) {
            throw new ResourceNotFoundException("Unidade não encontrada com o ID: " + unidadeId);
        }

        Pageable pageable = PageRequest.of(page, limit, Sort.by("produto.id").ascending());
        Page<Estoque> estoquePage = estoqueRepository.findCardapioByUnidadeId(unidadeId, pageable);

        return PaginatedResponseDto.fromPage(estoquePage, ProdutoCardapioResponseDto::fromEstoque);
    }
}
