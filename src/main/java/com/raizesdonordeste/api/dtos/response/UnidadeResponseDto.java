package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.Unidade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados cadastrais de uma unidade da rede Raízes do Nordeste")
public class UnidadeResponseDto {

    @Schema(description = "Identificador único da unidade", example = "1")
    private Long id;

    @Schema(description = "Nome fantasia da unidade", example = "Raízes do Nordeste - Recife Matriz")
    private String nome;

    @Schema(description = "Endereço completo da unidade", example = "Rua do Bom Jesus, 120 - Bairro do Recife, Recife - PE")
    private String endereco;

    @Schema(description = "Status de funcionamento da unidade", example = "true")
    private Boolean ativa;

    @Schema(description = "Data de cadastramento da unidade no sistema")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    public static UnidadeResponseDto fromEntity(Unidade unidade) {
        if (unidade == null) {
            return null;
        }
        return UnidadeResponseDto.builder()
                .id(unidade.getId())
                .nome(unidade.getNome())
                .endereco(unidade.getEndereco())
                .ativa(unidade.getAtiva())
                .dataCriacao(unidade.getDataCriacao())
                .build();
    }
}
