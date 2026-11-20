package com.raizesdonordeste.api.dtos.request;

import com.raizesdonordeste.domain.enums.TipoMovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para movimentação ou ajuste de estoque de um produto em uma unidade")
public class EstoqueUpdateRequestDto {

    @NotNull(message = "O ID da unidade é obrigatório")
    @Schema(description = "Identificador da unidade onde o estoque será atualizado", example = "1")
    private Long unidadeId;

    @NotNull(message = "O ID do produto é obrigatório")
    @Schema(description = "Identificador do produto cujo estoque será atualizado", example = "1")
    private Long produtoId;

    @NotNull(message = "O tipo de movimentação é obrigatório (ENTRADA, SAIDA ou AJUSTE)")
    @Schema(description = "Operação a ser realizada: ENTRADA (soma), SAIDA (subtrai) ou AJUSTE (define saldo)", example = "ENTRADA")
    private TipoMovimentacaoEstoque tipoMovimentacao;

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 0, message = "A quantidade não pode ser negativa")
    @Schema(description = "Quantidade de itens para movimentação (para ENTRADA/SAIDA deve ser >= 1; para AJUSTE pode ser >= 0)", example = "20")
    private Integer quantidade;

    @Size(max = 255, message = "O motivo não pode exceder 255 caracteres")
    @Schema(description = "Justificativa ou motivo da movimentação para fins de auditoria", example = "Recebimento de lote semanal de insumos do fornecedor")
    private String motivo;
}
