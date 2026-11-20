package com.raizesdonordeste.api.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Item participante de um pedido com produto e quantidade")
public class ItemPedidoRequestDto {

    @NotNull(message = "O ID do produto é obrigatório.")
    @Schema(description = "Identificador único do produto no cardápio", example = "1")
    private Long produtoId;

    @NotNull(message = "A quantidade do item é obrigatória.")
    @Min(value = 1, message = "A quantidade do item deve ser de no mínimo 1 unidade.")
    @Schema(description = "Quantidade de unidades solicitadas", example = "2")
    private Integer quantidade;
}
