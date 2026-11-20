package com.raizesdonordeste.api.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados para cadastro de novo produto no cardápio")
public class ProdutoCreateRequestDto {

    @NotBlank(message = "O nome do produto é obrigatório")
    @Size(max = 150, message = "O nome do produto não pode exceder 150 caracteres")
    @Schema(description = "Nome do prato ou bebida típica", example = "Escondidinho de Macaxeira com Charque")
    private String nome;

    @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
    @Schema(description = "Descrição detalhada dos ingredientes e modo de preparo", example = "Purê cremoso de macaxeira com manteiga de garrafa recheado com charque desfiada e queijo coalho gratinado.")
    private String descricao;

    @NotNull(message = "O preço base é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço base deve ser estritamente maior que zero")
    @Schema(description = "Preço base em reais", example = "42.00")
    private BigDecimal precoBase;

    @Builder.Default
    @Schema(description = "Status de ativação inicial do produto", example = "true")
    private Boolean ativo = true;
}
