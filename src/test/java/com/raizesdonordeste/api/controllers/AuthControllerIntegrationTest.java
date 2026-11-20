package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.api.dtos.request.RegisterRequestDto;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.domain.enums.Perfil;
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
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.raizesdonordeste.infrastructure.repositories.AuditoriaLogRepository auditoriaLogRepository;

    @Autowired(required = false)
    private com.raizesdonordeste.infrastructure.repositories.PedidoRepository pedidoRepository;

    @Autowired(required = false)
    private com.raizesdonordeste.infrastructure.repositories.PagamentoRepository pagamentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (pagamentoRepository != null) {
            pagamentoRepository.deleteAll();
        }
        if (pedidoRepository != null) {
            pedidoRepository.deleteAll();
        }
        auditoriaLogRepository.deleteAll();
        usuarioRepository.deleteAll();

        // Usuário Administrador para testes de RBAC
        usuarioRepository.save(Usuario.builder()
                .nome("Administrador Teste")
                .email("admin@teste.com.br")
                .senhaHash(passwordEncoder.encode("Admin@123"))
                .perfil(Perfil.ADMIN)
                .pontosFidelidade(0)
                .consentimentoLgpd(true)
                .build());

        // Usuário Cliente para testes de RBAC e LGPD
        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Teste")
                .email("cliente@teste.com.br")
                .senhaHash(passwordEncoder.encode("Cliente@123"))
                .perfil(Perfil.CLIENTE)
                .pontosFidelidade(50)
                .consentimentoLgpd(true)
                .build());
    }

    @Test
    @DisplayName("Deve cadastrar cliente com consentimento LGPD obrigatório e retornar HTTP 201")
    void deveRegistrarClienteComConsentimentoLgpd() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .nome("Novo Cliente do Sertão")
                .email("novo.cliente@teste.com.br")
                .senha("SenhaForte@2026")
                .consentimentoLgpd(true)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome").value("Novo Cliente do Sertão"))
                .andExpect(jsonPath("$.email").value("novo.cliente@teste.com.br"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.consentimentoLgpd").value(true))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com HTTP 422 quando consentimento LGPD for falso")
    void deveRejeitarCadastroSemConsentimentoLgpd() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .nome("Cliente Sem Aceite LGPD")
                .email("sem.lgpd@teste.com.br")
                .senha("SenhaForte@2026")
                .consentimentoLgpd(false)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("REGRA_NEGOCIO_INVALIDA"))
                .andExpect(jsonPath("$.message", containsString("LGPD")))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com HTTP 409 quando o e-mail já estiver em uso")
    void deveRejeitarCadastroComEmailDuplicado() throws Exception {
        RegisterRequestDto request = RegisterRequestDto.builder()
                .nome("Outro Cliente")
                .email("cliente@teste.com.br") // e-mail já cadastrado no setUp
                .senha("Senha@123456")
                .consentimentoLgpd(true)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_JA_CADASTRADO"))
                .andExpect(jsonPath("$.message", containsString("Já existe um usuário cadastrado")));
    }

    @Test
    @DisplayName("Deve autenticar com sucesso e retornar token JWT stateless e dados do usuário")
    void deveAutenticarComSucesso() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("admin@teste.com.br")
                .senha("Admin@123")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.usuario.email").value("admin@teste.com.br"))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"));
    }

    @Test
    @DisplayName("Deve recusar autenticação com HTTP 401 quando credenciais forem inválidas")
    void deveRecusarAutenticacaoComCredenciaisInvalidas() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("admin@teste.com.br")
                .senha("SenhaIncorreta@999")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTORIZADO"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 padronizado ao tentar acessar rota protegida sem token JWT")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTORIZADO"))
                .andExpect(jsonPath("$.message", containsString("token Bearer válido")))
                .andExpect(jsonPath("$.requestId", notNullValue()))
                .andExpect(jsonPath("$.path").value("/auth/me"));
    }

    @Test
    @DisplayName("Deve recuperar dados do usuário autenticado no /auth/me usando Bearer Token")
    void deveRecuperarUsuarioAutenticadoComToken() throws Exception {
        String token = obterToken("cliente@teste.com.br", "Cliente@123");

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("cliente@teste.com.br"))
                .andExpect(jsonPath("$.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.perfil").value("CLIENTE"));
    }

    @Test
    @DisplayName("RBAC: Perfil CLIENTE deve receber HTTP 403 ACESSO_NEGADO ao tentar rota restrita a ADMIN")
    void deveRetornar403QuandoClienteAcessarRotaAdmin() throws Exception {
        String tokenCliente = obterToken("cliente@teste.com.br", "Cliente@123");

        mockMvc.perform(get("/auth/admin-check")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.message", containsString("permissão")));
    }

    @Test
    @DisplayName("RBAC: Perfil ADMIN deve ter acesso autorizado (HTTP 200) em rota protegida de admin")
    void devePermitirAcessoAdminParaPerfilAdmin() throws Exception {
        String tokenAdmin = obterToken("admin@teste.com.br", "Admin@123");

        mockMvc.perform(get("/auth/admin-check")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTORIZADO"));
    }

    private String obterToken(String email, String senha) throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email(email)
                .senha(senha)
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseJson).get("accessToken").asText();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (pagamentoRepository != null) {
            pagamentoRepository.deleteAll();
        }
        if (pedidoRepository != null) {
            pedidoRepository.deleteAll();
        }
        auditoriaLogRepository.deleteAll();
        usuarioRepository.deleteAll();
    }
}
