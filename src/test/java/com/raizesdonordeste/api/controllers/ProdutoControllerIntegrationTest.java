package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.api.dtos.request.ProdutoCreateRequestDto;
import com.raizesdonordeste.domain.entities.Produto;
import com.raizesdonordeste.domain.entities.Unidade;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.domain.enums.Perfil;
import com.raizesdonordeste.infrastructure.repositories.EstoqueRepository;
import com.raizesdonordeste.infrastructure.repositories.ProdutoRepository;
import com.raizesdonordeste.infrastructure.repositories.UnidadeRepository;
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

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProdutoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private com.raizesdonordeste.infrastructure.repositories.AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenGerente;
    private String tokenCliente;
    private Produto produtoExistente;

    @BeforeEach
    void setUp() throws Exception {
        auditoriaLogRepository.deleteAll();
        estoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        unidadeRepository.deleteAll();
        usuarioRepository.deleteAll();

        unidadeRepository.save(Unidade.builder()
                .nome("Unidade Recife")
                .endereco("Rua das Flores, 100")
                .ativa(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Admin Geral")
                .email("admin.prod@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.ADMIN)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Gerente Geral")
                .email("gerente.prod@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.GERENTE)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Comum")
                .email("cliente.prod@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .consentimentoLgpd(true)
                .build());

        tokenAdmin = loginAndGetToken("admin.prod@teste.com.br", "Senha@123");
        tokenGerente = loginAndGetToken("gerente.prod@teste.com.br", "Senha@123");
        tokenCliente = loginAndGetToken("cliente.prod@teste.com.br", "Senha@123");

        produtoExistente = produtoRepository.save(Produto.builder()
                .nome("Carne de Sol com Macaxeira")
                .descricao("Carne de sol frita na manteiga com macaxeira")
                .precoBase(new BigDecimal("49.90"))
                .ativo(true)
                .build());
    }

    @Test
    @DisplayName("Deve listar produtos com paginação (HTTP 200)")
    void deveListarProdutosComPaginacao() throws Exception {
        mockMvc.perform(get("/produtos?page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].nome").value("Carne de Sol com Macaxeira"));
    }

    @Test
    @DisplayName("Deve consultar produto por ID existente (HTTP 200)")
    void deveConsultarProdutoPorId() throws Exception {
        mockMvc.perform(get("/produtos/" + produtoExistente.getId())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(produtoExistente.getId()))
                .andExpect(jsonPath("$.nome").value("Carne de Sol com Macaxeira"))
                .andExpect(jsonPath("$.precoBase").value(49.90));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 ao consultar produto com ID inexistente")
    void deveRetornar404ParaProdutoInexistente() throws Exception {
        mockMvc.perform(get("/produtos/999999")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Deve permitir cadastro de novo produto por ADMIN com HTTP 201")
    void deveCadastrarProdutoComAdmin() throws Exception {
        ProdutoCreateRequestDto request = ProdutoCreateRequestDto.builder()
                .nome("Moqueca de Camarão")
                .descricao("Camarões frescos com leite de coco e azeite de dendê")
                .precoBase(new BigDecimal("68.00"))
                .ativo(true)
                .build();

        mockMvc.perform(post("/produtos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nome").value("Moqueca de Camarão"))
                .andExpect(jsonPath("$.precoBase").value(68.00))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @DisplayName("Deve permitir cadastro de novo produto por GERENTE com HTTP 201")
    void deveCadastrarProdutoComGerente() throws Exception {
        ProdutoCreateRequestDto request = ProdutoCreateRequestDto.builder()
                .nome("Cuscuz com Carne Seca")
                .descricao("Cuscuz tradicional nordestino com carne seca")
                .precoBase(new BigDecimal("19.50"))
                .ativo(true)
                .build();

        mockMvc.perform(post("/produtos")
                        .header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Cuscuz com Carne Seca"));
    }

    @Test
    @DisplayName("RBAC: Deve recusar cadastro com HTTP 403 quando perfil for CLIENTE")
    void deveRecusarCadastroProdutoComCliente() throws Exception {
        ProdutoCreateRequestDto request = ProdutoCreateRequestDto.builder()
                .nome("Produto Indevido")
                .precoBase(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/produtos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Deve rejeitar produto com preço inválido ou nome em branco com HTTP 422")
    void deveRejeitarProdutoInvalido() throws Exception {
        ProdutoCreateRequestDto request = ProdutoCreateRequestDto.builder()
                .nome("")
                .precoBase(new BigDecimal("-5.00"))
                .build();

        mockMvc.perform(post("/produtos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Deve retornar HTTP 409 quando tentar cadastrar produto com nome duplicado")
    void deveRetornar409ParaNomeDuplicado() throws Exception {
        ProdutoCreateRequestDto request = ProdutoCreateRequestDto.builder()
                .nome("carne de sol com macaxeira") // mesmo nome do setup (case insensitive)
                .precoBase(new BigDecimal("55.00"))
                .build();

        mockMvc.perform(post("/produtos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PRODUTO_JA_CADASTRADO"));
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
