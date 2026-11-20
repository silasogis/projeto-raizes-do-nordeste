package com.raizesdonordeste.api.dtos.response;

import com.raizesdonordeste.domain.entities.ItemPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detalhamento de um item registrado no pedido")
public class ItemPedidoResponseDto {

    @Schema(description = "Identificador único do item no pedido", example = "1")
    private Long id;

    @Schema(description = "Identificador do produto", example = "1")
    private Long produtoId;

    @Schema(description = "Nome do produto no momento do pedido", example = "Baião de Dois Especial")
    private String produtoNome;

    @Schema(description = "Quantidade comprada", example = "2")
    private Integer quantidade;

    @Schema(description = "Preço unitário garantido no momento da criação do pedido", example = "38.50")
    private BigDecimal precoUnitarioGarantido;

    @Schema(description = "Subtotal calculado do item (quantidade * preço garantido)", example = "77.00")
    private BigDecimal subtotal;

    public static ItemPedidoResponseDto fromEntity(ItemPedido item) {
        if (item == null) {
            return null;
        }

        BigDecimal subtotal = (item.getPrecoUnitarioGarantido() != null && item.getQuantidade() != null)
                ? item.getPrecoUnitarioGarantido().multiply(BigDecimal.valueOf(item.getQuantidade()))
                : BigDecimal.ZERO;

        return ItemPedidoResponseDto.builder()
                .id(item.getId())
                .produtoId(item.getProduto() != null ? item.getProduto().getId() : null)
                .produtoNome(item.getProduto() != null ? item.getProduto().getNome() : null)
                .quantidade(item.getQuantidade())
                .precoUnitarioGarantido(item.getPrecoUnitarioGarantido())
                .subtotal(subtotal)
                .build();
    }
}
