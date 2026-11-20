package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.Pagamento;
import com.raizesdonordeste.domain.enums.MetodoPagamento;
import com.raizesdonordeste.domain.enums.StatusTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados do processamento de pagamento via Gateway Mock")
public class PagamentoResponseDto {

    @Schema(description = "Identificador único do pagamento", example = "1")
    private Long id;

    @Schema(description = "Identificador do pedido vinculado", example = "1")
    private Long pedidoId;

    @Schema(description = "Status resultante da transação", example = "APROVADO")
    private StatusTransacao statusTransacao;

    @Schema(description = "Forma de pagamento utilizada", example = "PIX")
    private MetodoPagamento metodoPagamento;

    @Schema(description = "Chave de idempotência da transação", example = "c56a4180-65aa-42ec-a945-5fd21dec0538")
    private String chaveIdempotencia;

    @Schema(description = "Payload JSON retornado pelo Gateway de Pagamento simulado")
    private String payloadResposta;

    @Schema(description = "Data e hora do processamento da transação")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataTransacao;

    public static PagamentoResponseDto fromEntity(Pagamento pagamento) {
        if (pagamento == null) {
            return null;
        }

        return PagamentoResponseDto.builder()
                .id(pagamento.getId())
                .pedidoId(pagamento.getPedido() != null ? pagamento.getPedido().getId() : null)
                .statusTransacao(pagamento.getStatusTransacao())
                .metodoPagamento(pagamento.getMetodoPagamento())
                .chaveIdempotencia(pagamento.getChaveIdempotencia())
                .payloadResposta(pagamento.getPayloadResposta())
                .dataTransacao(pagamento.getDataTransacao())
                .build();
    }
}
