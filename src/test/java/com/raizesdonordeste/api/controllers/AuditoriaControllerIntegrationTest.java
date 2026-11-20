package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.domain.entities.AuditoriaLog;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.domain.enums.Perfil;
import com.raizesdonordeste.infrastructure.repositories.AuditoriaLogRepository;
import com.raizesdonordeste.infrastructure.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenGerente;
    private String tokenCliente;

    @BeforeEach
    void setUp() throws Exception {
        auditoriaLogRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome("Admin Auditoria")
                .email("admin.auditoria@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.ADMIN)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Gerente Auditoria")
                .email("gerente.auditoria@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.GERENTE)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Auditoria")
                .email("cliente.auditoria@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .consentimentoLgpd(true)
                .build());

        tokenAdmin = loginAndGetToken("admin.auditoria@teste.com.br", "Senha@123");
        tokenGerente = loginAndGetToken("gerente.auditoria@teste.com.br", "Senha@123");
        tokenCliente = loginAndGetToken("cliente.auditoria@teste.com.br", "Senha@123");

        auditoriaLogRepository.save(AuditoriaLog.builder()
                .usuario(admin)
                .ator("admin.auditoria@teste.com.br")
                .acao("CRIACAO_PEDIDO")
                .entidade("PEDIDO")
                .entidadeId(101L)
                .detalhes("Pedido criado com 2 itens.")
                .build());

        auditoriaLogRepository.save(AuditoriaLog.builder()
                .usuario(admin)
                .ator("admin.auditoria@teste.com.br")
                .acao("PAGAMENTO_APROVADO")
                .entidade("PAGAMENTO")
                .entidadeId(202L)
                .detalhes("Pagamento aprovado via PIX.")
                .build());
    }

    @Test
    @DisplayName("Cenário 1: ADMIN deve consultar logs de auditoria com HTTP 200")
    void deveListarLogsComSucessoParaAdmin() throws Exception {
        mockMvc.perform(get("/auditoria?page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @DisplayName("Cenário 2: GERENTE deve consultar logs filtrados por entidade com HTTP 200")
    void deveListarLogsComFiltroEntidadeParaGerente() throws Exception {
        mockMvc.perform(get("/auditoria?entidade=PEDIDO&page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].entidade").value("PEDIDO"))
                .andExpect(jsonPath("$.items[0].entidadeId").value(101));
    }

    @Test
    @DisplayName("Cenário 3: Deve retornar HTTP 403 quando CLIENTE tentar consultar logs de auditoria")
    void deveRetornar403ParaCliente() throws Exception {
        mockMvc.perform(get("/auditoria")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Cenário 4: Deve retornar HTTP 401 sem token de autenticação")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTORIZADO"));
    }

    private String loginAndGetToken(String email, String senha) throws Exception {
        LoginRequestDto request = LoginRequestDto.builder().email(email).senha(senha).build();
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
