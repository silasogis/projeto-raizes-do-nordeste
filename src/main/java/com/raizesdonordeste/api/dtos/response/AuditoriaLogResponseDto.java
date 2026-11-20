package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.AuditoriaLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registro de auditoria para rastreabilidade de ações sensíveis")
public class AuditoriaLogResponseDto {

    @Schema(description = "Identificador único do log de auditoria", example = "1")
    private Long id;

    @Schema(description = "Identificador do usuário que realizou a ação (se autenticado)", example = "2")
    private Long usuarioId;

    @Schema(description = "Ator responsável pela operação (email, role ou SISTEMA)", example = "gerente.recife@raizesdonordeste.com.br")
    private String ator;

    @Schema(description = "Ação realizada", example = "CRIACAO_PEDIDO")
    private String acao;

    @Schema(description = "Nome da entidade afetada", example = "PEDIDO")
    private String entidade;

    @Schema(description = "Identificador da entidade afetada", example = "1")
    private Long entidadeId;

    @Schema(description = "Detalhamento e resumo da operação")
    private String detalhes;

    @Schema(description = "Data e hora do registro")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public static AuditoriaLogResponseDto fromEntity(AuditoriaLog log) {
        if (log == null) {
            return null;
        }

        return AuditoriaLogResponseDto.builder()
                .id(log.getId())
                .usuarioId(log.getUsuario() != null ? log.getUsuario().getId() : null)
                .ator(log.getAtor())
                .acao(log.getAcao())
                .entidade(log.getEntidade())
                .entidadeId(log.getEntidadeId())
                .detalhes(log.getDetalhes())
                .timestamp(log.getTimestamp())
                .build();
    }
}
