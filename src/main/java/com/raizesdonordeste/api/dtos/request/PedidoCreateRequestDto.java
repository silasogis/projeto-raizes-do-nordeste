package com.raizesdonordeste.api.dtos.request;

import com.raizesdonordeste.domain.enums.CanalPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Requisição de criação de novo pedido no fluxo multicanal")
public class PedidoCreateRequestDto {

    @NotNull(message = "O ID da unidade é obrigatório.")
    @Schema(description = "Identificador da unidade onde o pedido está sendo realizado", example = "1")
    private Long unidadeId;

    @Schema(description = "Identificador opcional do cliente cadastrado (se nulo, usa o usuário logado ou anônimo)", example = "3")
    private Long clienteId;

    @NotNull(message = "O canal do pedido é obrigatório.")
    @Schema(description = "Canal de atendimento de origem do pedido", example = "TOTEM", allowableValues = {"APP", "TOTEM", "BALCAO", "PICKUP", "WEB"})
    private CanalPedido canalPedido;

    @NotEmpty(message = "O pedido deve conter pelo menos um item.")
    @Valid
    @Schema(description = "Lista de itens a serem incluídos no pedido")
    private List<ItemPedidoRequestDto> itens;
}
