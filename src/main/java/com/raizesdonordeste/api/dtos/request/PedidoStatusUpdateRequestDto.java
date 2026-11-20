package com.raizesdonordeste.api.dtos.request;

import com.raizesdonordeste.domain.enums.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Requisição para atualização de status na esteira de preparo e atendimento do pedido")
public class PedidoStatusUpdateRequestDto {

    @NotNull(message = "O novo status do pedido é obrigatório.")
    @Schema(description = "Novo status desejado para o pedido", example = "PRONTO", allowableValues = {"PREPARACAO", "PRONTO", "ENTREGUE", "CANCELADO"})
    private StatusPedido novoStatus;

    @Schema(description = "Justificativa ou motivo da alteração de status (obrigatório em cancelamentos operacionais)", example = "Pedido concluído pela cozinha e pronto para entrega.")
    private String motivo;
}
