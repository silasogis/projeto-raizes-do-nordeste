package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.request.PedidoCreateRequestDto;
import com.raizesdonordeste.api.dtos.request.PedidoStatusUpdateRequestDto;
import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.PaginatedResponseDto;
import com.raizesdonordeste.api.dtos.response.PedidoResponseDto;
import com.raizesdonordeste.application.services.PedidoService;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
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
@RequestMapping("/pedidos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Pedidos", description = "Endpoints para criação multicanal, consulta e esteira de preparo de pedidos")
@SecurityRequirement(name = "BearerAuth")
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @Operation(
            summary = "Criar novo pedido (Fluxo Crítico Multicanal)",
            description = "Cria um novo pedido com validação atômica de estoque por unidade, cálculo de valores no servidor, registro de canal (APP, TOTEM, BALCAO, PICKUP, WEB) e auditoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso em status AGUARDANDO_PAGAMENTO"),
            @ApiResponse(responseCode = "400", description = "Requisição malformada ou parâmetro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Unidade ou Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Estoque insuficiente para um dos itens ou unidade inativa",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Falha de validação dos campos obrigatórios",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PedidoResponseDto> criarPedido(@Valid @RequestBody PedidoCreateRequestDto request) {
        PedidoResponseDto response = pedidoService.criarPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar pedidos com filtros multicanal",
            description = "Retorna listagem paginada de pedidos com filtros opcionais por canal de origem e status. Clientes visualizam apenas seus próprios pedidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista paginada de pedidos retornada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros de paginação ou filtro inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PaginatedResponseDto<PedidoResponseDto>> listarPedidos(
            @Parameter(description = "Filtro opcional por canal do pedido", example = "TOTEM")
            @RequestParam(required = false) CanalPedido canalPedido,
            @Parameter(description = "Filtro opcional por status do pedido", example = "PREPARACAO")
            @RequestParam(required = false) StatusPedido status,
            @Parameter(description = "Filtro opcional por unidade física", example = "1")
            @RequestParam(required = false) Long unidadeId,
            @Parameter(description = "Índice da página (inicia em 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "O parâmetro 'page' deve ser maior ou igual a 0") int page,
            @Parameter(description = "Quantidade de pedidos por página", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "O parâmetro 'limit' deve ser de no mínimo 1")
            @Max(value = 100, message = "O parâmetro 'limit' não pode exceder 100") int limit) {

        PaginatedResponseDto<PedidoResponseDto> response = pedidoService.listarPedidos(canalPedido, status, unidadeId, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar detalhes de um pedido",
            description = "Consulta as informações consolidadas de um pedido específico incluindo itens, preços garantidos e pagamento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido localizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado (cliente tentando visualizar pedido de outro usuário)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PedidoResponseDto> buscarPorId(
            @Parameter(description = "Identificador único do pedido", example = "1")
            @PathVariable Long id) {

        PedidoResponseDto response = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COZINHA', 'ATENDENTE', 'GERENTE', 'ADMIN')")
    @Operation(
            summary = "Atualizar status da esteira do pedido",
            description = "Avança o status de um pedido (PREPARACAO -> PRONTO -> ENTREGUE ou CANCELADO). Restrito a perfis operacionais e de gestão. Registra auditoria e acumula pontos de fidelidade na entrega."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status do pedido atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido ou corpo malformado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Acesso proibido para o perfil atual",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Transição de status inválida ou pedido já finalizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Validação dos campos falhou",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<PedidoResponseDto> atualizarStatus(
            @Parameter(description = "Identificador único do pedido", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody PedidoStatusUpdateRequestDto request) {

        PedidoResponseDto response = pedidoService.atualizarStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
