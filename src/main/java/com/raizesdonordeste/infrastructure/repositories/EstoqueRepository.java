package com.raizesdonordeste.infrastructure.repositories;

import com.raizesdonordeste.domain.entities.Estoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    Optional<Estoque> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    List<Estoque> findByUnidadeId(Long unidadeId);

    Page<Estoque> findByUnidadeId(Long unidadeId, Pageable pageable);

    @Query(value = "SELECT e FROM Estoque e JOIN FETCH e.produto p WHERE e.unidade.id = :unidadeId AND p.ativo = true",
           countQuery = "SELECT COUNT(e) FROM Estoque e WHERE e.unidade.id = :unidadeId AND e.produto.ativo = true")
    Page<Estoque> findCardapioByUnidadeId(@Param("unidadeId") Long unidadeId, Pageable pageable);

    @Query("SELECT e FROM Estoque e JOIN FETCH e.produto p WHERE e.unidade.id = :unidadeId AND p.ativo = true")
    List<Estoque> findCardapioByUnidadeId(@Param("unidadeId") Long unidadeId);

    boolean existsByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);
}
