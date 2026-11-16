package com.raizesdonordeste.domain.entities;

import com.raizesdonordeste.domain.enums.MetodoPagamento;
import com.raizesdonordeste.domain.enums.StatusTransacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_transacao", nullable = false, length = 30)
    private StatusTransacao statusTransacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pagamento", nullable = false, length = 30)
    private MetodoPagamento metodoPagamento;

    @Column(name = "chave_idempotencia", nullable = false, unique = true, length = 100)
    private String chaveIdempotencia;

    @Column(name = "payload_resposta", columnDefinition = "JSON")
    private String payloadResposta;

    @CreationTimestamp
    @Column(name = "data_transacao", nullable = false, updatable = false)
    private LocalDateTime dataTransacao;
}
