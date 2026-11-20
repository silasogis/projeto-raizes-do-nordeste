package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.request.EstoqueUpdateRequestDto;
import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.EstoqueResponseDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.application.services.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/estoque")
@RequiredArgsConstructor
@Validated
@Tag(name = "Estoque por Unidade", description = "Endpoints para consulta de saldo físico e movimentação de estoque com auditoria")
@SecurityRequirement(name = "BearerAuth")
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    @Operation(
            summary = "Consultar saldo de estoque por unidade",
            description = "Retorna a listagem paginada do saldo de estoque de todos os produtos vinculados à unidade informada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo de estoque retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetro unidadeId ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unidade informada não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<EstoqueResponseDto>> consultarEstoquePorUnidade(
            @Parameter(description = "Identificador da unidade a consultar", required = true, example = "1")
            @RequestParam @NotNull(message = "O parâmetro 'unidadeId' é obrigatório") Long unidadeId,
            @Parameter(description = "Índice da página (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "O parâmetro 'page' deve ser maior ou igual a 0") int page,
            @Parameter(description = "Quantidade de itens por página", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O parâmetro 'limit' deve ser de no mínimo 1")
            @Max(value = 100, message = "O parâmetro 'limit' não pode exceder 100") int limit) {

        PaginatedResponseDto<EstoqueResponseDto> response = estoqueService.consultarEstoquePorUnidade(unidadeId, page, limit);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(
            summary = "Atualizar estoque / Entrada, Saída ou Ajuste (Restrito a ADMIN e GERENTE)",
            description = "Executa a movimentação física de estoque (ENTRADA, SAIDA ou AJUSTE) de um produto em uma unidade, validando não-negatividade e registrando rastreabilidade em auditoria_logs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estoque atualizado e auditado com sucesso",
                    content = @Content(schema = @Schema(implementation = EstoqueResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requisição malformada ou dados inconsistentes",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado: perfil sem permissão (exige ADMIN ou GERENTE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unidade ou Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Conflito: saldo de estoque insuficiente para saída",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Erro de validação nos campos informados",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<EstoqueResponseDto> atualizarEstoque(@Valid @RequestBody EstoqueUpdateRequestDto request) {
        EstoqueResponseDto response = estoqueService.atualizarEstoque(request);
        return ResponseEntity.ok(response);
    }
}
