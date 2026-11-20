package com.raizesdonordeste.infrastructure.gateway;

import com.raizesdonordeste.domain.entities.Pedido;
import com.raizesdonordeste.domain.enums.MetodoPagamento;
import com.raizesdonordeste.domain.enums.StatusTransacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
public class GatewayPagamentoMock {

    public ProcessamentoPagamentoResult processar(
            Pedido pedido,
            MetodoPagamento metodoPagamento,
            String chaveIdempotencia,
            StatusTransacao statusSimulado
    ) {
        log.info("Processando pagamento MOCK para o Pedido ID {} via {}. Chave idempotência: {}",
                pedido.getId(), metodoPagamento, chaveIdempotencia);

        StatusTransacao statusFinal = (statusSimulado != null) ? statusSimulado : StatusTransacao.APROVADO;
        String transacaoId = "TX-MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String timestamp = Instant.now().toString();

        String payloadJson;
        if (statusFinal == StatusTransacao.APROVADO) {
            String codigoAutorizacao = "AUTH-" + Math.abs(chaveIdempotencia.hashCode() % 1000000);
            payloadJson = String.format(
                    "{\"gateway\":\"MOCK_PAYMENT_GATEWAY\",\"transacaoId\":\"%s\",\"pedidoId\":%d,\"status\":\"APROVADO\",\"metodo\":\"%s\",\"valor\":%s,\"codigoAutorizacao\":\"%s\",\"chaveIdempotencia\":\"%s\",\"processadoEm\":\"%s\"}",
                    transacaoId, pedido.getId(), metodoPagamento, pedido.getValorTotal(), codigoAutorizacao, chaveIdempotencia, timestamp
            );
        } else {
            payloadJson = String.format(
                    "{\"gateway\":\"MOCK_PAYMENT_GATEWAY\",\"transacaoId\":\"%s\",\"pedidoId\":%d,\"status\":\"RECUSADO\",\"metodo\":\"%s\",\"valor\":%s,\"motivo\":\"Transação não autorizada pela instituição emissora\",\"chaveIdempotencia\":\"%s\",\"processadoEm\":\"%s\"}",
                    transacaoId, pedido.getId(), metodoPagamento, pedido.getValorTotal(), chaveIdempotencia, timestamp
            );
        }

        log.info("Resultado do processamento MOCK: Status: {}, Transação: {}", statusFinal, transacaoId);

        return ProcessamentoPagamentoResult.builder()
                .statusTransacao(statusFinal)
                .transacaoId(transacaoId)
                .payloadResposta(payloadJson)
                .build();
    }
}
