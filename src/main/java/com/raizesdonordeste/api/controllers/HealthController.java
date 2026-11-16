package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.domain.exceptions.BusinessConflictException;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health Check", description = "Endpoints de verificação de integridade e diagnósticos da API")
public class HealthController {

    @GetMapping
    @Operation(summary = "Verificar integridade da aplicação", description = "Retorna status UP indicando que a API Spring Boot está ativa.")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "raizes-nordeste-backend",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/test-404")
    @Operation(summary = "Teste de erro 404 (ResourceNotFound)", description = "Simula a ocorrência de recurso não localizado.")
    public ResponseEntity<Void> testNotFound() {
        throw new ResourceNotFoundException("Recurso de diagnóstico não foi localizado no sistema.");
    }

    @GetMapping("/test-409")
    @Operation(summary = "Teste de erro 409 (Conflict)", description = "Simula conflito de negócio (ex: estoque insuficiente).")
    public ResponseEntity<Void> testConflict() {
        throw new BusinessConflictException("ESTOQUE_INSUFICIENTE", "Saldo de estoque insuficiente para o produto solicitado.");
    }

    @GetMapping("/test-500")
    @Operation(summary = "Teste de erro 500 (InternalServerError)", description = "Simula falha inesperada tratada pelo handler global.")
    public ResponseEntity<Void> testInternalError() {
        throw new RuntimeException("Falha simulada para verificação do tratamento de erro 500.");
    }
}
