package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.Pedido;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.StatusPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados consolidados de um pedido")
public class PedidoResponseDto {

    @Schema(description = "Identificador único do pedido", example = "1")
    private Long id;

    @Schema(description = "Identificador do cliente (se identificado)", example = "3")
    private Long clienteId;

    @Schema(description = "Nome do cliente (se identificado)", example = "João da Silva")
    private String clienteNome;

    @Schema(description = "Identificador da unidade de atendimento", example = "1")
    private Long unidadeId;

    @Schema(description = "Nome da unidade de atendimento", example = "Raízes Recife - Boa Viagem")
    private String unidadeNome;

    @Schema(description = "Canal de origem do pedido", example = "TOTEM")
    private CanalPedido canalPedido;

    @Schema(description = "Status atual na esteira de atendimento", example = "AGUARDANDO_PAGAMENTO")
    private StatusPedido status;

    @Schema(description = "Valor total do pedido calculado no servidor", example = "77.00")
    private BigDecimal valorTotal;

    @Schema(description = "Lista detalhada de itens que compõem o pedido")
    private List<ItemPedidoResponseDto> itens;

    @Schema(description = "Informações do pagamento (se já processado)")
    private PagamentoResponseDto pagamento;

    @Schema(description = "Data e hora de criação do pedido")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do pedido")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    public static PedidoResponseDto fromEntity(Pedido pedido) {
        if (pedido == null) {
            return null;
        }

        List<ItemPedidoResponseDto> itensDto = pedido.getItens() != null
                ? pedido.getItens().stream()
                        .map(ItemPedidoResponseDto::fromEntity)
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return PedidoResponseDto.builder()
                .id(pedido.getId())
                .clienteId(pedido.getCliente() != null ? pedido.getCliente().getId() : null)
                .clienteNome(pedido.getCliente() != null ? pedido.getCliente().getNome() : null)
                .unidadeId(pedido.getUnidade() != null ? pedido.getUnidade().getId() : null)
                .unidadeNome(pedido.getUnidade() != null ? pedido.getUnidade().getNome() : null)
                .canalPedido(pedido.getCanalPedido())
                .status(pedido.getStatus())
                .valorTotal(pedido.getValorTotal())
                .itens(itensDto)
                .pagamento(pedido.getPagamento() != null ? PagamentoResponseDto.fromEntity(pedido.getPagamento()) : null)
                .dataCriacao(pedido.getDataCriacao())
                .dataAtualizacao(pedido.getDataAtualizacao())
                .build();
    }
}
