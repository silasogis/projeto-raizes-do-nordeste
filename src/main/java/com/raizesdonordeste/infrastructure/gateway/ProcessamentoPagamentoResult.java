package com.raizesdonordeste.infrastructure.gateway;

import com.raizesdonordeste.domain.enums.StatusTransacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessamentoPagamentoResult {
    private StatusTransacao statusTransacao;
    private String transacaoId;
    private String payloadResposta;
}
