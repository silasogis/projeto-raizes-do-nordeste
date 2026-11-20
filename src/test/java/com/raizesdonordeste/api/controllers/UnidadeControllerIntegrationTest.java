package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.domain.entities.Estoque;
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
class UnidadeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private com.raizesdonordeste.infrastructure.repositories.AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenCliente;
    private Unidade unidadeRecife;
    private Produto baiao;

    @BeforeEach
    void setUp() throws Exception {
        auditoriaLogRepository.deleteAll();
        estoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        unidadeRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Teste")
                .email("cliente.unidade@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .pontosFidelidade(10)
                .consentimentoLgpd(true)
                .build());

        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email("cliente.unidade@teste.com.br")
                .senha("Senha@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        tokenCliente = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        unidadeRecife = unidadeRepository.save(Unidade.builder()
                .nome("Raízes do Nordeste - Recife Matriz")
                .endereco("Rua do Bom Jesus, 120, Recife - PE")
                .ativa(true)
                .build());

        unidadeRepository.save(Unidade.builder()
                .nome("Raízes do Nordeste - Salvador Pelourinho")
                .endereco("Largo do Pelourinho, 45, Salvador - BA")
                .ativa(true)
                .build());

        baiao = produtoRepository.save(Produto.builder()
                .nome("Baião de Dois Especial")
                .descricao("Arroz, feijão de corda e queijo coalho")
                .precoBase(new BigDecimal("38.50"))
                .ativo(true)
                .build());

        estoqueRepository.save(Estoque.builder()
                .unidade(unidadeRecife)
                .produto(baiao)
                .quantidadeDisponivel(50)
                .build());
    }

    @Test
    @DisplayName("Deve listar unidades ativas com paginação (HTTP 200)")
    void deveListarUnidadesComPaginacao() throws Exception {
        mockMvc.perform(get("/unidades?page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].nome", containsString("Recife")));
    }

    @Test
    @DisplayName("Deve buscar unidade por ID existente com HTTP 200")
    void deveBuscarUnidadePorId() throws Exception {
        mockMvc.perform(get("/unidades/" + unidadeRecife.getId())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(unidadeRecife.getId()))
                .andExpect(jsonPath("$.nome").value("Raízes do Nordeste - Recife Matriz"))
                .andExpect(jsonPath("$.endereco", containsString("Bom Jesus")));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 ao buscar unidade com ID inexistente")
    void deveRetornar404ParaUnidadeInexistente() throws Exception {
        mockMvc.perform(get("/unidades/999999")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.message", containsString("Unidade não encontrada")));
    }

    @Test
    @DisplayName("Deve listar cardápio da unidade com saldo de estoque em tempo real (HTTP 200)")
    void deveListarCardapioDaUnidadeComEstoque() throws Exception {
        mockMvc.perform(get("/unidades/" + unidadeRecife.getId() + "/produtos?page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(baiao.getId()))
                .andExpect(jsonPath("$.items[0].nome").value("Baião de Dois Especial"))
                .andExpect(jsonPath("$.items[0].precoBase").value(38.50))
                .andExpect(jsonPath("$.items[0].quantidadeDisponivel").value(50))
                .andExpect(jsonPath("$.items[0].unidadeId").value(unidadeRecife.getId()));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 ao acessar /unidades sem token JWT")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/unidades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTORIZADO"));
    }
}
