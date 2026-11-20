package com.raizesdonordeste.infrastructure.repositories;

import com.raizesdonordeste.domain.entities.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByPedidoId(Long pedidoId);

    Optional<Pagamento> findByChaveIdempotencia(String chaveIdempotencia);

    boolean existsByChaveIdempotencia(String chaveIdempotencia);

    boolean existsByPedidoId(Long pedidoId);
}
