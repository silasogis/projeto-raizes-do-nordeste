package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.api.dtos.response.ProdutoCardapioResponseDto;
import com.raizesdonordeste.api.dtos.response.UnidadeResponseDto;
import com.raizesdonordeste.application.services.UnidadeService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
@Validated
@Tag(name = "Unidades & Cardápio Local", description = "Endpoints para consulta de unidades da rede e cardápio disponível por loja")
@SecurityRequirement(name = "BearerAuth")
public class UnidadeController {

    private final UnidadeService unidadeService;

    @GetMapping
    @Operation(
            summary = "Listar unidades da rede",
            description = "Retorna a listagem paginada de todas as lojas e filiais ativas da rede Raízes do Nordeste."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de unidades retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token JWT ausente ou expirado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<UnidadeResponseDto>> listarUnidades(
            @Parameter(description = "Índice da página (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "O parâmetro 'page' deve ser maior ou igual a 0") int page,
            @Parameter(description = "Quantidade de registros por página", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O parâmetro 'limit' deve ser de no mínimo 1")
            @Max(value = 100, message = "O parâmetro 'limit' não pode exceder 100") int limit) {

        PaginatedResponseDto<UnidadeResponseDto> response = unidadeService.listarUnidades(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar unidade por ID",
            description = "Recupera os detalhes cadastrais de uma unidade específica a partir do seu identificador numérico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unidade localizada com sucesso",
                    content = @Content(schema = @Schema(implementation = UnidadeResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unidade não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<UnidadeResponseDto> buscarPorId(
            @Parameter(description = "Identificador da unidade", example = "1")
            @PathVariable Long id) {

        UnidadeResponseDto response = unidadeService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/produtos")
    @Operation(
            summary = "Consultar cardápio disponível na unidade",
            description = "Retorna a listagem paginada de produtos ativos ofertados pela unidade informada, acompanhados do saldo atual de estoque em tempo real."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cardápio da unidade retornado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unidade não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<ProdutoCardapioResponseDto>> listarCardapioUnidade(
            @Parameter(description = "Identificador da unidade", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Índice da página (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "O parâmetro 'page' deve ser maior ou igual a 0") int page,
            @Parameter(description = "Quantidade de itens por página", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O parâmetro 'limit' deve ser de no mínimo 1")
            @Max(value = 100, message = "O parâmetro 'limit' não pode exceder 100") int limit) {

        PaginatedResponseDto<ProdutoCardapioResponseDto> response = unidadeService.listarCardapioUnidade(id, page, limit);
        return ResponseEntity.ok(response);
    }
}
