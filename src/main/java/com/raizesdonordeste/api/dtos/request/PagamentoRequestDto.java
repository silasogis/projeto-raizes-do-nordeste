package com.raizesdonordeste.api.dtos.request;

import com.raizesdonordeste.domain.enums.MetodoPagamento;
import com.raizesdonordeste.domain.enums.StatusTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Requisição para processamento de pagamento via Gateway Mock com idempotência")
public class PagamentoRequestDto {

    @NotNull(message = "O ID do pedido é obrigatório.")
    @Schema(description = "Identificador do pedido aguardando pagamento", example = "1")
    private Long pedidoId;

    @NotNull(message = "O método de pagamento é obrigatório.")
    @Schema(description = "Forma de pagamento selecionada", example = "PIX", allowableValues = {"CARTAO_CREDITO", "CARTAO_DEBITO", "PIX", "DINHEIRO"})
    private MetodoPagamento metodoPagamento;

    @NotBlank(message = "A chave de idempotência é obrigatória.")
    @Schema(description = "Chave única da transação para proteção contra duplicidade de cobrança (ex: UUID)", example = "c56a4180-65aa-42ec-a945-5fd21dec0538")
    private String chaveIdempotencia;

    @Schema(description = "Permite simular o resultado do mock para testes (APROVADO ou RECUSADO). Padrão: APROVADO", example = "APROVADO", allowableValues = {"APROVADO", "RECUSADO"})
    private StatusTransacao simularStatus;
}
