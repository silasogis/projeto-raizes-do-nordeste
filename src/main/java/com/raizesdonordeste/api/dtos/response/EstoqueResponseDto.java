package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.Estoque;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados de saldo e controle de estoque de um produto em uma unidade")
public class EstoqueResponseDto {

    @Schema(description = "Identificador único do registro de estoque", example = "1")
    private Long id;

    @Schema(description = "Identificador da unidade", example = "1")
    private Long unidadeId;

    @Schema(description = "Nome da unidade", example = "Raízes do Nordeste - Recife Matriz")
    private String unidadeNome;

    @Schema(description = "Identificador do produto", example = "1")
    private Long produtoId;

    @Schema(description = "Nome do produto", example = "Baião de Dois Especial")
    private String produtoNome;

    @Schema(description = "Quantidade física disponível no estoque", example = "50")
    private Integer quantidadeDisponivel;

    @Schema(description = "Data e hora da última movimentação ou atualização")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    public static EstoqueResponseDto fromEntity(Estoque estoque) {
        if (estoque == null) {
            return null;
        }
        return EstoqueResponseDto.builder()
                .id(estoque.getId())
                .unidadeId(estoque.getUnidade().getId())
                .unidadeNome(estoque.getUnidade().getNome())
                .produtoId(estoque.getProduto().getId())
                .produtoNome(estoque.getProduto().getNome())
                .quantidadeDisponivel(estoque.getQuantidadeDisponivel())
                .dataAtualizacao(estoque.getDataAtualizacao())
                .build();
    }
}
