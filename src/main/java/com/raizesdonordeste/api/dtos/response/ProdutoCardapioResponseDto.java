package com.raizesdonordeste.api.dtos.response;

import com.raizesdonordeste.domain.entities.Estoque;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Item do cardápio disponível em uma unidade específica com saldo em tempo real")
public class ProdutoCardapioResponseDto {

    @Schema(description = "Identificador do produto", example = "1")
    private Long id;

    @Schema(description = "Nome do prato ou bebida", example = "Baião de Dois Especial")
    private String nome;

    @Schema(description = "Descrição detalhada dos ingredientes", example = "Arroz, feijão de corda, queijo coalho e carne de sol.")
    private String descricao;

    @Schema(description = "Preço de venda vigente", example = "38.50")
    private BigDecimal precoBase;

    @Schema(description = "Quantidade de porções disponíveis em estoque nesta unidade", example = "50")
    private Integer quantidadeDisponivel;

    @Schema(description = "Indica se o produto está ativo no cardápio", example = "true")
    private Boolean ativo;

    @Schema(description = "Identificador da unidade que oferta este produto", example = "1")
    private Long unidadeId;

    public static ProdutoCardapioResponseDto fromEstoque(Estoque estoque) {
        if (estoque == null) {
            return null;
        }
        return ProdutoCardapioResponseDto.builder()
                .id(estoque.getProduto().getId())
                .nome(estoque.getProduto().getNome())
                .descricao(estoque.getProduto().getDescricao())
                .precoBase(estoque.getProduto().getPrecoBase())
                .quantidadeDisponivel(estoque.getQuantidadeDisponivel())
                .ativo(estoque.getProduto().getAtivo())
                .unidadeId(estoque.getUnidade().getId())
                .build();
    }
}
