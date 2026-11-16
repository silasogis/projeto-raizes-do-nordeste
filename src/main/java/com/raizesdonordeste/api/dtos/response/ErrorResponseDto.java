package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estrutura padrão de resposta de erro da API")
public class ErrorResponseDto {

    @Schema(description = "Código identificador do tipo de erro", example = "VALIDACAO_FALHOU")
    private String error;

    @Schema(description = "Mensagem explicativa legível para humanos", example = "Um ou mais campos contêm erros de validação")
    private String message;

    @Schema(description = "Lista detalhada dos erros ou campos afetados")
    private List<String> details;

    @Schema(description = "Momento exato da ocorrência do erro em formato ISO-8601 UTC", example = "2026-11-16T19:00:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    @Schema(description = "URI da rota onde ocorreu a falha", example = "/pedidos")
    private String path;

    @Schema(description = "Identificador único da requisição para fins de rastreabilidade e auditoria", example = "c1f7a2d8-4b2e-4b2a-89a1-7c9d3e5f1a2b")
    private String requestId;
}
