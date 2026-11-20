package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.Produto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados cadastrais de um produto do cardápio")
public class ProdutoResponseDto {

    @Schema(description = "Identificador único do produto", example = "1")
    private Long id;

    @Schema(description = "Nome do produto ou prato típico", example = "Baião de Dois Especial")
    private String nome;

    @Schema(description = "Descrição detalhada dos ingredientes", example = "Arroz, feijão de corda, queijo coalho e carne de sol.")
    private String descricao;

    @Schema(description = "Preço base de venda em reais", example = "38.50")
    private BigDecimal precoBase;

    @Schema(description = "Status de disponibilidade do produto no catálogo", example = "true")
    private Boolean ativo;

    @Schema(description = "Data de cadastramento do produto no sistema")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    public static ProdutoResponseDto fromEntity(Produto produto) {
        if (produto == null) {
            return null;
        }
        return ProdutoResponseDto.builder()
                .id(produto.getId())
                .nome(produto.getNome())
                .descricao(produto.getDescricao())
                .precoBase(produto.getPrecoBase())
                .ativo(produto.getAtivo())
                .dataCriacao(produto.getDataCriacao())
                .build();
    }
}
