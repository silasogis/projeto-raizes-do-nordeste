package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.response.AuditoriaLogResponseDto;
import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.application.services.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@Validated
@Tag(name = "Auditoria & Logs", description = "Endpoints para rastreabilidade de ações sensíveis no sistema (Restrito a ADMIN e GERENTE)")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    @Operation(
            summary = "Listar registros de auditoria",
            description = "Consulta o histórico de logs de auditoria gravados para ações críticas (criação de pedido, pagamentos, esteira de status, movimentações de estoque). Exclusivo para ADMIN e GERENTE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de logs de auditoria retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado para o perfil atual",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<AuditoriaLogResponseDto>> listarLogs(
            @Parameter(description = "Filtrar por nome da entidade (ex: PEDIDO, PAGAMENTO, ESTOQUE)", example = "PEDIDO")
            @RequestParam(required = false) String entidade,
            @Parameter(description = "Filtrar pelo ID da entidade específica", example = "1")
            @RequestParam(required = false) Long entidadeId,
            @Parameter(description = "Índice da página (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "O parâmetro 'page' deve ser maior ou igual a 0") int page,
            @Parameter(description = "Quantidade de registros por página", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O parâmetro 'limit' deve ser de no mínimo 1")
            @Max(value = 100, message = "O parâmetro 'limit' não pode exceder 100") int limit) {

        PaginatedResponseDto<AuditoriaLogResponseDto> response = auditoriaService.listarLogs(entidade, entidadeId, page, limit);
        return ResponseEntity.ok(response);
    }
}
