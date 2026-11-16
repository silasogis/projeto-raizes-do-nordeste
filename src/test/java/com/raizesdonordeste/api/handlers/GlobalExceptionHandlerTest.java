package com.raizesdonordeste.api.handlers;

import com.raizesdonordeste.api.controllers.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Health check deve retornar HTTP 200 com status UP")
    void deveRetornarStatusUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("raizes-nordeste-backend"));
    }

    @Test
    @DisplayName("Erro 404 deve retornar JSON padrão com RECURSO_NAO_ENCONTRADO")
    void deveRetornarErro404Padronizado() throws Exception {
        mockMvc.perform(get("/health/test-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.path").value("/health/test-404"));
    }

    @Test
    @DisplayName("Erro 409 deve retornar JSON padrão com código específico de conflito")
    void deveRetornarErro409Padronizado() throws Exception {
        mockMvc.perform(get("/health/test-409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"))
                .andExpect(jsonPath("$.message", containsString("insuficiente")))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    @DisplayName("Erro 500 deve retornar JSON padrão com ERRO_INTERNO")
    void deveRetornarErro500Padronizado() throws Exception {
        mockMvc.perform(get("/health/test-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }
}
