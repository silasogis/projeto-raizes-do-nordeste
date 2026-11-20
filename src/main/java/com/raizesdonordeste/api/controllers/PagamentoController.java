package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.request.PagamentoRequestDto;
import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.PagamentoResponseDto;
import com.raizesdonordeste.application.services.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pagamentos (Mock)", description = "Endpoints para simulação de pagamento desacoplado com controle de idempotência e estorno")
@SecurityRequirement(name = "BearerAuth")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @Operation(
            summary = "Processar pagamento via Gateway Mock",
            description = "Simula o envio e retorno de pagamento para um pedido em AGUARDANDO_PAGAMENTO. Exige chave de idempotência para evitar duplicidade. Em caso de aprovação avança o pedido para PREPARACAO; em caso de recusa cancela o pedido e estorna o saldo de estoque."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transação de pagamento processada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Corpo de requisição ou parâmetros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Chave de idempotência duplicada ou pedido já pago/não elegível",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Falha de validação dos campos obrigatórios",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PagamentoResponseDto> processarPagamento(@Valid @RequestBody PagamentoRequestDto request) {
        PagamentoResponseDto response = pagamentoService.processarPagamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar transação de pagamento por ID",
            description = "Consulta o registro persistido de uma transação de pagamento incluindo status e payload retornado pelo gateway mock."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento localizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PagamentoResponseDto> buscarPorId(
            @Parameter(description = "Identificador único do pagamento", example = "1")
            @PathVariable Long id) {

        PagamentoResponseDto response = pagamentoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pedido/{pedidoId}")
    @Operation(
            summary = "Buscar transação de pagamento pelo ID do pedido",
            description = "Consulta a transação de pagamento associada a determinado pedido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento do pedido localizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Nenhum pagamento associado ao pedido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PagamentoResponseDto> buscarPorPedidoId(
            @Parameter(description = "Identificador único do pedido", example = "1")
            @PathVariable Long pedidoId) {

        PagamentoResponseDto response = pagamentoService.buscarPorPedidoId(pedidoId);
        return ResponseEntity.ok(response);
    }
}
