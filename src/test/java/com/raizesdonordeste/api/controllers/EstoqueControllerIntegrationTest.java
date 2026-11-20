package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.EstoqueUpdateRequestDto;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.domain.entities.AuditoriaLog;
import com.raizesdonordeste.domain.entities.Estoque;
import com.raizesdonordeste.domain.entities.Produto;
import com.raizesdonordeste.domain.entities.Unidade;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.domain.enums.Perfil;
import com.raizesdonordeste.domain.enums.TipoMovimentacaoEstoque;
import com.raizesdonordeste.infrastructure.repositories.AuditoriaLogRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstoqueControllerIntegrationTest {

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
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenGerente;
    private String tokenCliente;
    private Unidade unidade;
    private Produto produto;
    private Estoque estoque;

    @BeforeEach
    void setUp() throws Exception {
        auditoriaLogRepository.deleteAll();
        estoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        unidadeRepository.deleteAll();
        usuarioRepository.deleteAll();

        unidade = unidadeRepository.save(Unidade.builder()
                .nome("Raízes do Nordeste - Recife Matriz")
                .endereco("Rua do Bom Jesus, 120, Recife - PE")
                .ativa(true)
                .build());

        produto = produtoRepository.save(Produto.builder()
                .nome("Acarajé Tradicional")
                .descricao("Bolinhos de feijão fradinho com vatapá e camarão")
                .precoBase(new BigDecimal("22.00"))
                .ativo(true)
                .build());

        estoque = estoqueRepository.save(Estoque.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeDisponivel(30)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Admin Estoque")
                .email("admin.estoque@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.ADMIN)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Gerente Estoque")
                .email("gerente.estoque@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.GERENTE)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Cliente Sem Acesso Estoque")
                .email("cliente.estoque@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .consentimentoLgpd(true)
                .build());

        tokenAdmin = loginAndGetToken("admin.estoque@teste.com.br", "Senha@123");
        tokenGerente = loginAndGetToken("gerente.estoque@teste.com.br", "Senha@123");
        tokenCliente = loginAndGetToken("cliente.estoque@teste.com.br", "Senha@123");
    }

    @Test
    @DisplayName("Deve consultar estoque por unidade informada com HTTP 200")
    void deveConsultarEstoquePorUnidade() throws Exception {
        mockMvc.perform(get("/estoque?unidadeId=" + unidade.getId() + "&page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].unidadeId").value(unidade.getId()))
                .andExpect(jsonPath("$.items[0].produtoId").value(produto.getId()))
                .andExpect(jsonPath("$.items[0].quantidadeDisponivel").value(30));
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 quando o parâmetro unidadeId não for fornecido")
    void deveRetornar400SemParametroUnidadeId() throws Exception {
        mockMvc.perform(get("/estoque")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PARAMETRO_OBRIGATORIO_AUSENTE"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 ao consultar estoque de unidade inexistente")
    void deveRetornar404ParaUnidadeInexistente() throws Exception {
        mockMvc.perform(get("/estoque?unidadeId=999999")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Deve realizar ENTRADA de estoque por ADMIN, atualizar saldo e registrar auditoria (HTTP 200)")
    void deveRealizarEntradaEstoqueComAdmin() throws Exception {
        EstoqueUpdateRequestDto request = EstoqueUpdateRequestDto.builder()
                .unidadeId(unidade.getId())
                .produtoId(produto.getId())
                .tipoMovimentacao(TipoMovimentacaoEstoque.ENTRADA)
                .quantidade(20)
                .motivo("Recebimento de lote de feijão e azeite de dendê")
                .build();

        mockMvc.perform(patch("/estoque")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeDisponivel").value(50)) // 30 + 20
                .andExpect(jsonPath("$.unidadeId").value(unidade.getId()))
                .andExpect(jsonPath("$.produtoId").value(produto.getId()));

        List<AuditoriaLog> logs = auditoriaLogRepository.findByEntidadeAndEntidadeId("ESTOQUE", estoque.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAcao()).isEqualTo("MOVIMENTACAO_ESTOQUE_ENTRADA");
        assertThat(logs.get(0).getAtor()).isEqualTo("admin.estoque@teste.com.br");
        assertThat(logs.get(0).getDetalhes()).contains("Quantidade movimentada: 20");
    }

    @Test
    @DisplayName("Deve realizar SAIDA de estoque por GERENTE e atualizar saldo (HTTP 200)")
    void deveRealizarSaidaEstoqueComGerente() throws Exception {
        EstoqueUpdateRequestDto request = EstoqueUpdateRequestDto.builder()
                .unidadeId(unidade.getId())
                .produtoId(produto.getId())
                .tipoMovimentacao(TipoMovimentacaoEstoque.SAIDA)
                .quantidade(10)
                .motivo("Avaria de insumo perecível")
                .build();

        mockMvc.perform(patch("/estoque")
                        .header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeDisponivel").value(20)); // 30 - 10

        List<AuditoriaLog> logs = auditoriaLogRepository.findByEntidadeAndEntidadeId("ESTOQUE", estoque.getId());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getAcao()).isEqualTo("MOVIMENTACAO_ESTOQUE_SAIDA");
        assertThat(logs.get(0).getAtor()).isEqualTo("gerente.estoque@teste.com.br");
    }

    @Test
    @DisplayName("Deve recusar SAIDA de estoque com HTTP 409 quando o saldo for insuficiente")
    void deveRecusarSaidaComEstoqueInsuficiente() throws Exception {
        EstoqueUpdateRequestDto request = EstoqueUpdateRequestDto.builder()
                .unidadeId(unidade.getId())
                .produtoId(produto.getId())
                .tipoMovimentacao(TipoMovimentacaoEstoque.SAIDA)
                .quantidade(100) // saldo atual é 30
                .motivo("Tentativa de baixa excedente")
                .build();

        mockMvc.perform(patch("/estoque")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"))
                .andExpect(jsonPath("$.message", containsString("Saldo insuficiente")));
    }

    @Test
    @DisplayName("Deve realizar AJUSTE de inventário por ADMIN com HTTP 200")
    void deveRealizarAjusteEstoque() throws Exception {
        EstoqueUpdateRequestDto request = EstoqueUpdateRequestDto.builder()
                .unidadeId(unidade.getId())
                .produtoId(produto.getId())
                .tipoMovimentacao(TipoMovimentacaoEstoque.AJUSTE)
                .quantidade(15) // define diretamente para 15
                .motivo("Contagem de inventário físico mensal")
                .build();

        mockMvc.perform(patch("/estoque")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeDisponivel").value(15));
    }

    @Test
    @DisplayName("RBAC: Deve retornar HTTP 403 quando perfil CLIENTE tentar atualizar estoque")
    void deveRetornar403ParaClienteAtualizandoEstoque() throws Exception {
        EstoqueUpdateRequestDto request = EstoqueUpdateRequestDto.builder()
                .unidadeId(unidade.getId())
                .produtoId(produto.getId())
                .tipoMovimentacao(TipoMovimentacaoEstoque.ENTRADA)
                .quantidade(10)
                .build();

        mockMvc.perform(patch("/estoque")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 401 ao tentar atualizar estoque sem token JWT")
    void deveRetornar401SemToken() throws Exception {
        EstoqueUpdateRequestDto request = EstoqueUpdateRequestDto.builder()
                .unidadeId(unidade.getId())
                .produtoId(produto.getId())
                .tipoMovimentacao(TipoMovimentacaoEstoque.ENTRADA)
                .quantidade(10)
                .build();

        mockMvc.perform(patch("/estoque")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
