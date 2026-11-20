package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.request.ProdutoCreateRequestDto;
import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.api.dtos.response.ProdutoResponseDto;
import com.raizesdonordeste.application.services.ProdutoService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/produtos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Produtos & Catálogo", description = "Endpoints para consulta do catálogo de produtos e cadastro restrito à gestão")
@SecurityRequirement(name = "BearerAuth")
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    @Operation(
            summary = "Listar produtos ativos do catálogo",
            description = "Retorna a listagem paginada de todos os produtos ativos cadastrados no sistema Raízes do Nordeste."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de produtos retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<ProdutoResponseDto>> listarProdutos(
            @Parameter(description = "Índice da página (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "O parâmetro 'page' deve ser maior ou igual a 0") int page,
            @Parameter(description = "Quantidade de produtos por página", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O parâmetro 'limit' deve ser de no mínimo 1")
            @Max(value = 100, message = "O parâmetro 'limit' não pode exceder 100") int limit) {

        PaginatedResponseDto<ProdutoResponseDto> response = produtoService.listarProdutos(page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar produto por ID",
            description = "Recupera os detalhes de um produto específico cadastrado no cardápio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto recuperado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProdutoResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ProdutoResponseDto> buscarPorId(
            @Parameter(description = "Identificador do produto", example = "1")
            @PathVariable Long id) {

        ProdutoResponseDto response = produtoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    @Operation(
            summary = "Cadastrar novo produto (Restrito a ADMIN e GERENTE)",
            description = "Cadastra um novo produto no cardápio da rede e inicializa o saldo de estoque nas unidades ativas. Operação restrita a administradores e gerentes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProdutoResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado: perfil sem permissão (exige ADMIN ou GERENTE)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Conflito: produto com mesmo nome já existente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Falha de validação dos campos obrigatórios",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ProdutoResponseDto> criarProduto(@Valid @RequestBody ProdutoCreateRequestDto request) {
        ProdutoResponseDto response = produtoService.criarProduto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
