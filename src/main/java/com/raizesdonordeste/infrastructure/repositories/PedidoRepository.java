package com.raizesdonordeste.infrastructure.repositories;

import com.raizesdonordeste.domain.entities.Pedido;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query(value = "SELECT p FROM Pedido p WHERE " +
                   "(:canalPedido IS NULL OR p.canalPedido = :canalPedido) AND " +
                   "(:status IS NULL OR p.status = :status) AND " +
                   "(:unidadeId IS NULL OR p.unidade.id = :unidadeId) AND " +
                   "(:clienteId IS NULL OR p.cliente.id = :clienteId)",
           countQuery = "SELECT COUNT(p) FROM Pedido p WHERE " +
                        "(:canalPedido IS NULL OR p.canalPedido = :canalPedido) AND " +
                        "(:status IS NULL OR p.status = :status) AND " +
                        "(:unidadeId IS NULL OR p.unidade.id = :unidadeId) AND " +
                        "(:clienteId IS NULL OR p.cliente.id = :clienteId)")
    Page<Pedido> findWithFilters(
            @Param("canalPedido") CanalPedido canalPedido,
            @Param("status") StatusPedido status,
            @Param("unidadeId") Long unidadeId,
            @Param("clienteId") Long clienteId,
            Pageable pageable
    );

    @Query("SELECT p FROM Pedido p " +
           "JOIN FETCH p.unidade u " +
           "LEFT JOIN FETCH p.cliente c " +
           "LEFT JOIN FETCH p.itens i " +
           "LEFT JOIN FETCH i.produto pr " +
           "WHERE p.id = :id")
    Optional<Pedido> findByIdWithDetails(@Param("id") Long id);
}
