package com.raizesdonordeste.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "estoque",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_estoque_unidade_produto", columnNames = {"unidade_id", "produto_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "quantidade_disponivel", nullable = false)
    @Builder.Default
    private Integer quantidadeDisponivel = 0;

    @UpdateTimestamp
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    public void adicionarEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade a adicionar deve ser maior que zero.");
        }
        this.quantidadeDisponivel += quantidade;
    }

    public void subtrairEstoque(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade a subtrair deve ser maior que zero.");
        }
        if (this.quantidadeDisponivel < quantidade) {
            String nomeProduto = (produto != null && produto.getNome() != null) ? produto.getNome() : "ID " + (produto != null ? produto.getId() : "desconhecido");
            Long idUnidade = (unidade != null) ? unidade.getId() : null;
            throw new com.raizesdonordeste.domain.exceptions.BusinessConflictException(
                    "ESTOQUE_INSUFICIENTE",
                    String.format("Saldo insuficiente em estoque para o produto '%s' na unidade ID %s. Solicitado: %d, Disponível: %d.",
                            nomeProduto, idUnidade, quantidade, this.quantidadeDisponivel)
            );
        }
        this.quantidadeDisponivel -= quantidade;
    }

    public void definirEstoque(int novaQuantidade) {
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("A quantidade de estoque não pode ser negativa.");
        }
        this.quantidadeDisponivel = novaQuantidade;
    }
}
