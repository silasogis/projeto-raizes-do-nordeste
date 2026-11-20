package com.raizesdonordeste.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de movimentação física ou lógica de estoque")
public enum TipoMovimentacaoEstoque {
    @Schema(description = "Entrada de novos itens / reabastecimento")
    ENTRADA,

    @Schema(description = "Saída de itens / perda / descarte")
    SAIDA,

    @Schema(description = "Ajuste direto do inventário para contagem real")
    AJUSTE
}
