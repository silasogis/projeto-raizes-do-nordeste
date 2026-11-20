package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.ItemPedidoRequestDto;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.api.dtos.request.PedidoCreateRequestDto;
import com.raizesdonordeste.api.dtos.request.PedidoStatusUpdateRequestDto;
import com.raizesdonordeste.domain.entities.*;
import com.raizesdonordeste.domain.enums.CanalPedido;
import com.raizesdonordeste.domain.enums.Perfil;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.infrastructure.repositories.*;
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
import java.util.Collections;
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
class PedidoControllerIntegrationTest {

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
    private PedidoRepository pedidoRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private AuditoriaLogRepository auditoriaLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenCliente1;
    private String tokenCliente2;
    private String tokenAtendente;
    private String tokenCozinha;
    private String tokenGerente;

    private Unidade unidade;
    private Produto produto1;
    private Produto produto2;
    private Usuario cliente1;
    private Usuario cliente2;

    @BeforeEach
    void setUp() throws Exception {
        auditoriaLogRepository.deleteAll();
        pagamentoRepository.deleteAll();
        pedidoRepository.deleteAll();
        estoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        unidadeRepository.deleteAll();
        usuarioRepository.deleteAll();

        unidade = unidadeRepository.save(Unidade.builder()
                .nome("Raízes Recife - Boa Viagem")
                .endereco("Av. Boa Viagem, 1500, Recife - PE")
                .ativa(true)
                .build());

        produto1 = produtoRepository.save(Produto.builder()
                .nome("Baião de Dois Especial")
                .descricao("Feijão de corda, arroz, carne de sol e queijo coalho")
                .precoBase(new BigDecimal("35.00"))
                .ativo(true)
                .build());

        produto2 = produtoRepository.save(Produto.builder()
                .nome("Suco de Caju da Terra")
                .descricao("Polpa natural da fruta regional")
                .precoBase(new BigDecimal("10.00"))
                .ativo(true)
                .build());

        // Saldo de estoque inicial
        estoqueRepository.save(Estoque.builder()
                .unidade(unidade)
                .produto(produto1)
                .quantidadeDisponivel(20)
                .build());

        estoqueRepository.save(Estoque.builder()
                .unidade(unidade)
                .produto(produto2)
                .quantidadeDisponivel(15)
                .build());

        cliente1 = usuarioRepository.save(Usuario.builder()
                .nome("Maria Silva")
                .email("maria.silva@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .pontosFidelidade(0)
                .consentimentoLgpd(true)
                .build());

        cliente2 = usuarioRepository.save(Usuario.builder()
                .nome("Carlos Eduardo")
                .email("carlos.eduardo@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .pontosFidelidade(10)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Atendente Balcao")
                .email("atendente.balcao@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.ATENDENTE)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Chef Cozinha")
                .email("chef.cozinha@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.COZINHA)
                .consentimentoLgpd(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Gerente Geral")
                .email("gerente.geral@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.GERENTE)
                .consentimentoLgpd(true)
                .build());

        tokenCliente1 = loginAndGetToken("maria.silva@teste.com.br", "Senha@123");
        tokenCliente2 = loginAndGetToken("carlos.eduardo@teste.com.br", "Senha@123");
        tokenAtendente = loginAndGetToken("atendente.balcao@teste.com.br", "Senha@123");
        tokenCozinha = loginAndGetToken("chef.cozinha@teste.com.br", "Senha@123");
        tokenGerente = loginAndGetToken("gerente.geral@teste.com.br", "Senha@123");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        auditoriaLogRepository.deleteAll();
        pagamentoRepository.deleteAll();
        pedidoRepository.deleteAll();
        estoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        unidadeRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Cenário 1: Deve criar pedido válido com sucesso (HTTP 201) via canal TOTEM e baixar estoque")
    void deveCriarPedidoComSucesso() throws Exception {
        PedidoCreateRequestDto request = PedidoCreateRequestDto.builder()
                .unidadeId(unidade.getId())
                .canalPedido(CanalPedido.TOTEM)
                .itens(List.of(
                        ItemPedidoRequestDto.builder().produtoId(produto1.getId()).quantidade(2).build(), // 2 * 35.00 = 70.00
                        ItemPedidoRequestDto.builder().produtoId(produto2.getId()).quantidade(1).build()  // 1 * 10.00 = 10.00
                ))
                .build();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.canalPedido").value("TOTEM"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
                .andExpect(jsonPath("$.valorTotal").value(80.00))
                .andExpect(jsonPath("$.itens", hasSize(2)))
                .andExpect(jsonPath("$.clienteId").value(cliente1.getId()));

        // Verifica que o estoque foi baixado
        Estoque estoqueP1 = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto1.getId()).orElseThrow();
        assertThat(estoqueP1.getQuantidadeDisponivel()).isEqualTo(18); // 20 - 2

        Estoque estoqueP2 = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto2.getId()).orElseThrow();
        assertThat(estoqueP2.getQuantidadeDisponivel()).isEqualTo(14); // 15 - 1

        // Verifica log de auditoria
        List<AuditoriaLog> logs = auditoriaLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "CRIACAO_PEDIDO".equals(l.getAcao()) && "PEDIDO".equals(l.getEntidade()));
    }

    @Test
    @DisplayName("Cenário 2: Deve retornar HTTP 422 quando canalPedido for ausente na criação")
    void deveRetornar422QuandoCanalPedidoAusente() throws Exception {
        PedidoCreateRequestDto request = PedidoCreateRequestDto.builder()
                .unidadeId(unidade.getId())
                .canalPedido(null) // Campo obrigatório ausente
                .itens(List.of(ItemPedidoRequestDto.builder().produtoId(produto1.getId()).quantidade(1).build()))
                .build();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Cenário 3: Deve retornar HTTP 422 quando quantidade do item for zero ou negativa")
    void deveRetornar422QuandoQuantidadeInvalida() throws Exception {
        PedidoCreateRequestDto request = PedidoCreateRequestDto.builder()
                .unidadeId(unidade.getId())
                .canalPedido(CanalPedido.APP)
                .itens(List.of(ItemPedidoRequestDto.builder().produtoId(produto1.getId()).quantidade(0).build()))
                .build();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDACAO_FALHOU"));
    }

    @Test
    @DisplayName("Cenário 4: Deve retornar HTTP 404 quando produto informado não existir")
    void deveRetornar404QuandoProdutoInexistente() throws Exception {
        PedidoCreateRequestDto request = PedidoCreateRequestDto.builder()
                .unidadeId(unidade.getId())
                .canalPedido(CanalPedido.BALCAO)
                .itens(List.of(ItemPedidoRequestDto.builder().produtoId(9999L).quantidade(1).build()))
                .build();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Cenário 5: Deve retornar HTTP 404 quando unidade informada não existir")
    void deveRetornar404QuandoUnidadeInexistente() throws Exception {
        PedidoCreateRequestDto request = PedidoCreateRequestDto.builder()
                .unidadeId(9999L)
                .canalPedido(CanalPedido.WEB)
                .itens(List.of(ItemPedidoRequestDto.builder().produtoId(produto1.getId()).quantidade(1).build()))
                .build();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Cenário 6: Deve retornar HTTP 409 (ESTOQUE_INSUFICIENTE) se saldo for menor que a quantidade solicitada")
    void deveRetornar409QuandoEstoqueInsuficiente() throws Exception {
        PedidoCreateRequestDto request = PedidoCreateRequestDto.builder()
                .unidadeId(unidade.getId())
                .canalPedido(CanalPedido.APP)
                .itens(List.of(ItemPedidoRequestDto.builder().produtoId(produto1.getId()).quantidade(25).build())) // Temos apenas 20
                .build();

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"))
                .andExpect(jsonPath("$.message", containsString("Saldo insuficiente em estoque")));

        // Verifica que o saldo não foi alterado
        Estoque estoqueP1 = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto1.getId()).orElseThrow();
        assertThat(estoqueP1.getQuantidadeDisponivel()).isEqualTo(20);
    }

    @Test
    @DisplayName("Cenário 7: Deve listar pedidos com paginação e filtro por canal (?canalPedido=TOTEM)")
    void deveListarPedidosComFiltroCanal() throws Exception {
        criarPedidoDireto(cliente1, CanalPedido.TOTEM, StatusPedido.PREPARACAO);
        criarPedidoDireto(cliente1, CanalPedido.APP, StatusPedido.AGUARDANDO_PAGAMENTO);

        mockMvc.perform(get("/pedidos?canalPedido=TOTEM&page=0&limit=10")
                        .header("Authorization", "Bearer " + tokenGerente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].canalPedido").value("TOTEM"));
    }

    @Test
    @DisplayName("Cenário 8: Deve buscar detalhes do pedido por ID com HTTP 200")
    void deveBuscarPedidoPorIdComSucesso() throws Exception {
        Pedido pedido = criarPedidoDireto(cliente1, CanalPedido.WEB, StatusPedido.AGUARDANDO_PAGAMENTO);

        mockMvc.perform(get("/pedidos/" + pedido.getId())
                        .header("Authorization", "Bearer " + tokenCliente1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido.getId()))
                .andExpect(jsonPath("$.canalPedido").value("WEB"))
                .andExpect(jsonPath("$.itens", hasSize(1)));
    }

    @Test
    @DisplayName("Cenário 9: Deve retornar HTTP 403 quando cliente tentar visualizar pedido de outro usuário")
    void deveRetornar403QuandoClienteTentarVisualizarPedidoDeOutro() throws Exception {
        Pedido pedidoDoCliente1 = criarPedidoDireto(cliente1, CanalPedido.APP, StatusPedido.AGUARDANDO_PAGAMENTO);

        mockMvc.perform(get("/pedidos/" + pedidoDoCliente1.getId())
                        .header("Authorization", "Bearer " + tokenCliente2)) // Cliente 2 tentando ver pedido do Cliente 1
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Cenário 10: Perfil COZINHA deve atualizar status para PRONTO com HTTP 200")
    void deveAtualizarStatusParaProntoPorPerfilCozinha() throws Exception {
        Pedido pedido = criarPedidoDireto(cliente1, CanalPedido.TOTEM, StatusPedido.PREPARACAO);

        PedidoStatusUpdateRequestDto request = PedidoStatusUpdateRequestDto.builder()
                .novoStatus(StatusPedido.PRONTO)
                .motivo("Pedido finalizado na grelha.")
                .build();

        mockMvc.perform(patch("/pedidos/" + pedido.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenCozinha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PRONTO"));

        Pedido atualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(atualizado.getStatus()).isEqualTo(StatusPedido.PRONTO);
    }

    @Test
    @DisplayName("Cenário 11: Deve atualizar status para ENTREGUE e creditar pontos de fidelidade (LGPD consentido)")
    void deveAtualizarStatusParaEntregueEAcumularFidelidade() throws Exception {
        Pedido pedido = criarPedidoDireto(cliente1, CanalPedido.BALCAO, StatusPedido.PRONTO); // total 70.00

        PedidoStatusUpdateRequestDto request = PedidoStatusUpdateRequestDto.builder()
                .novoStatus(StatusPedido.ENTREGUE)
                .motivo("Entregue ao cliente no balcão.")
                .build();

        mockMvc.perform(patch("/pedidos/" + pedido.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenAtendente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));

        // Verifica fidelidade creditada ao cliente (70 pontos por R$ 70.00)
        Usuario clienteAtualizado = usuarioRepository.findById(cliente1.getId()).orElseThrow();
        assertThat(clienteAtualizado.getPontosFidelidade()).isEqualTo(70);

        // Verifica auditoria de fidelidade
        List<AuditoriaLog> logs = auditoriaLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "ACUMULO_FIDELIDADE".equals(l.getAcao()));
    }

    @Test
    @DisplayName("Cenário 12: Deve retornar HTTP 409 em transição inválida de status (ex: PRONTO -> PREPARACAO)")
    void deveRetornar409EmTransicaoDeStatusInvalida() throws Exception {
        Pedido pedido = criarPedidoDireto(cliente1, CanalPedido.PICKUP, StatusPedido.PRONTO);

        PedidoStatusUpdateRequestDto request = PedidoStatusUpdateRequestDto.builder()
                .novoStatus(StatusPedido.PREPARACAO)
                .motivo("Tentando voltar status")
                .build();

        mockMvc.perform(patch("/pedidos/" + pedido.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRANSICAO_STATUS_INVALIDA"));
    }

    @Test
    @DisplayName("Cenário 13: Deve retornar HTTP 403 se CLIENTE tentar atualizar status do pedido")
    void deveRetornar403AoTentarAtualizarStatusComoCliente() throws Exception {
        Pedido pedido = criarPedidoDireto(cliente1, CanalPedido.APP, StatusPedido.PREPARACAO);

        PedidoStatusUpdateRequestDto request = PedidoStatusUpdateRequestDto.builder()
                .novoStatus(StatusPedido.PRONTO)
                .build();

        mockMvc.perform(patch("/pedidos/" + pedido.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenCliente1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Cenário 14: Deve cancelar pedido e estornar estoque reservado")
    void deveCancelarPedidoEEstornarEstoque() throws Exception {
        Pedido pedido = criarPedidoDireto(cliente1, CanalPedido.TOTEM, StatusPedido.PREPARACAO);

        // Saldo atual de produto1 é 20 - 2 = 18
        Estoque estoqueAntes = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto1.getId()).orElseThrow();
        estoqueAntes.setQuantidadeDisponivel(18);
        estoqueRepository.save(estoqueAntes);

        PedidoStatusUpdateRequestDto request = PedidoStatusUpdateRequestDto.builder()
                .novoStatus(StatusPedido.CANCELADO)
                .motivo("Cliente solicitou desistência do pedido.")
                .build();

        mockMvc.perform(patch("/pedidos/" + pedido.getId() + "/status")
                        .header("Authorization", "Bearer " + tokenGerente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        // Verifica que o estoque foi estornado (+2) voltando para 20
        Estoque estoqueDepois = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto1.getId()).orElseThrow();
        assertThat(estoqueDepois.getQuantidadeDisponivel()).isEqualTo(20);
    }

    private Pedido criarPedidoDireto(Usuario cliente, CanalPedido canal, StatusPedido status) {
        Pedido pedido = Pedido.builder()
                .unidade(unidade)
                .cliente(cliente)
                .canalPedido(canal)
                .status(status)
                .valorTotal(new BigDecimal("70.00"))
                .build();

        ItemPedido item = ItemPedido.builder()
                .produto(produto1)
                .quantidade(2)
                .precoUnitarioGarantido(new BigDecimal("35.00"))
                .build();

        pedido.adicionarItem(item);
        return pedidoRepository.save(pedido);
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
