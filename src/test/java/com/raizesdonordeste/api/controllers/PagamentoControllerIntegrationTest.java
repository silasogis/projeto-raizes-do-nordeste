package com.raizesdonordeste.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.api.dtos.request.PagamentoRequestDto;
import com.raizesdonordeste.domain.entities.*;
import com.raizesdonordeste.domain.enums.*;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagamentoControllerIntegrationTest {

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

    private String tokenCliente;
    private Unidade unidade;
    private Produto produto;
    private Usuario cliente;
    private Pedido pedido;

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
                .nome("Raízes Fortaleza - Beira Mar")
                .endereco("Av. Beira Mar, 2100, Fortaleza - CE")
                .ativa(true)
                .build());

        produto = produtoRepository.save(Produto.builder()
                .nome("Carne de Sol com Macaxeira")
                .descricao("Acompanha manteiga de garrafa e queijo coalho")
                .precoBase(new BigDecimal("45.00"))
                .ativo(true)
                .build());

        estoqueRepository.save(Estoque.builder()
                .unidade(unidade)
                .produto(produto)
                .quantidadeDisponivel(10)
                .build());

        cliente = usuarioRepository.save(Usuario.builder()
                .nome("Fernanda Lima")
                .email("fernanda.lima@teste.com.br")
                .senhaHash(passwordEncoder.encode("Senha@123"))
                .perfil(Perfil.CLIENTE)
                .consentimentoLgpd(true)
                .build());

        tokenCliente = loginAndGetToken("fernanda.lima@teste.com.br", "Senha@123");

        pedido = Pedido.builder()
                .unidade(unidade)
                .cliente(cliente)
                .canalPedido(CanalPedido.APP)
                .status(StatusPedido.AGUARDANDO_PAGAMENTO)
                .valorTotal(new BigDecimal("45.00"))
                .build();

        ItemPedido item = ItemPedido.builder()
                .produto(produto)
                .quantidade(1)
                .precoUnitarioGarantido(new BigDecimal("45.00"))
                .build();

        pedido.adicionarItem(item);
        pedido = pedidoRepository.save(pedido);
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
    @DisplayName("Cenário 1: Deve processar pagamento APROVADO com sucesso (HTTP 201), avançando pedido para PREPARACAO")
    void deveProcessarPagamentoAprovadoComSucesso() throws Exception {
        String chaveIdempotencia = UUID.randomUUID().toString();

        PagamentoRequestDto request = PagamentoRequestDto.builder()
                .pedidoId(pedido.getId())
                .metodoPagamento(MetodoPagamento.PIX)
                .chaveIdempotencia(chaveIdempotencia)
                .simularStatus(StatusTransacao.APROVADO)
                .build();

        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.pedidoId").value(pedido.getId()))
                .andExpect(jsonPath("$.statusTransacao").value("APROVADO"))
                .andExpect(jsonPath("$.metodoPagamento").value("PIX"))
                .andExpect(jsonPath("$.chaveIdempotencia").value(chaveIdempotencia))
                .andExpect(jsonPath("$.payloadResposta", containsString("MOCK_PAYMENT_GATEWAY")));

        // Verifica que o pedido avançou para PREPARACAO
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(pedidoAtualizado.getStatus()).isEqualTo(StatusPedido.PREPARACAO);

        // Verifica log de auditoria
        List<AuditoriaLog> logs = auditoriaLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "PAGAMENTO_APROVADO".equals(l.getAcao()));
    }

    @Test
    @DisplayName("Cenário 2: Deve retornar HTTP 409 quando chave de idempotência for duplicada")
    void deveRetornar409QuandoChaveIdempotenciaDuplicada() throws Exception {
        String chaveIdempotencia = "IDEMPOTENCIA-UNICA-123456";

        PagamentoRequestDto request = PagamentoRequestDto.builder()
                .pedidoId(pedido.getId())
                .metodoPagamento(MetodoPagamento.CARTAO_CREDITO)
                .chaveIdempotencia(chaveIdempotencia)
                .simularStatus(StatusTransacao.APROVADO)
                .build();

        // Primeira chamada bem-sucedida
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Segunda chamada com a mesma chave deve ser rejeitada com 409
        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CHAVE_IDEMPOTENCIA_DUPLICADA"));
    }

    @Test
    @DisplayName("Cenário 3: Deve retornar HTTP 409 se pedido já tiver pagamento processado")
    void deveRetornar409QuandoPedidoNaoEstiverAguardandoPagamento() throws Exception {
        pedido.setStatus(StatusPedido.PREPARACAO);
        pedidoRepository.save(pedido);

        PagamentoRequestDto request = PagamentoRequestDto.builder()
                .pedidoId(pedido.getId())
                .metodoPagamento(MetodoPagamento.CARTAO_DEBITO)
                .chaveIdempotencia(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PAGAMENTO_JA_PROCESSADO"));
    }

    @Test
    @DisplayName("Cenário 4: Deve processar pagamento RECUSADO, cancelar pedido e estornar estoque")
    void deveProcessarPagamentoRecusadoECancelarPedidoComEstornoEstoque() throws Exception {
        // Saldo atual de estoque é 9 (considerando 1 reservado para o pedido)
        Estoque estoque = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
        estoque.setQuantidadeDisponivel(9);
        estoqueRepository.save(estoque);

        PagamentoRequestDto request = PagamentoRequestDto.builder()
                .pedidoId(pedido.getId())
                .metodoPagamento(MetodoPagamento.CARTAO_CREDITO)
                .chaveIdempotencia(UUID.randomUUID().toString())
                .simularStatus(StatusTransacao.RECUSADO)
                .build();

        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusTransacao").value("RECUSADO"))
                .andExpect(jsonPath("$.payloadResposta", containsString("Transação não autorizada")));

        // Verifica que o pedido foi CANCELADO
        Pedido pedidoAtualizado = pedidoRepository.findById(pedido.getId()).orElseThrow();
        assertThat(pedidoAtualizado.getStatus()).isEqualTo(StatusPedido.CANCELADO);

        // Verifica que o estoque foi estornado (+1) voltando a 10
        Estoque estoqueDepois = estoqueRepository.findByUnidadeIdAndProdutoId(unidade.getId(), produto.getId()).orElseThrow();
        assertThat(estoqueDepois.getQuantidadeDisponivel()).isEqualTo(10);

        // Verifica auditoria de recusa e cancelamento
        List<AuditoriaLog> logs = auditoriaLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "PAGAMENTO_RECUSADO".equals(l.getAcao()));
        assertThat(logs).anyMatch(l -> "CANCELAMENTO_PEDIDO".equals(l.getAcao()));
    }

    @Test
    @DisplayName("Cenário 5: Deve buscar pagamento por ID com HTTP 200")
    void deveBuscarPagamentoPorId() throws Exception {
        Pagamento pagamento = pagamentoRepository.save(Pagamento.builder()
                .pedido(pedido)
                .metodoPagamento(MetodoPagamento.PIX)
                .statusTransacao(StatusTransacao.APROVADO)
                .chaveIdempotencia("BUSCA-ID-12345")
                .payloadResposta("{\"status\":\"APROVADO\"}")
                .build());

        mockMvc.perform(get("/pagamentos/" + pagamento.getId())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pagamento.getId()))
                .andExpect(jsonPath("$.chaveIdempotencia").value("BUSCA-ID-12345"));
    }

    @Test
    @DisplayName("Cenário 6: Deve buscar pagamento associado ao pedido com HTTP 200")
    void deveBuscarPagamentoPorPedidoId() throws Exception {
        pagamentoRepository.save(Pagamento.builder()
                .pedido(pedido)
                .metodoPagamento(MetodoPagamento.DINHEIRO)
                .statusTransacao(StatusTransacao.APROVADO)
                .chaveIdempotencia("PEDIDO-LINK-999")
                .payloadResposta("{\"status\":\"APROVADO\"}")
                .build());

        mockMvc.perform(get("/pagamentos/pedido/" + pedido.getId())
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedidoId").value(pedido.getId()))
                .andExpect(jsonPath("$.metodoPagamento").value("DINHEIRO"));
    }

    @Test
    @DisplayName("Cenário 7: Deve retornar HTTP 404 ao tentar pagar pedido inexistente")
    void deveRetornar404QuandoPedidoInexistente() throws Exception {
        PagamentoRequestDto request = PagamentoRequestDto.builder()
                .pedidoId(9999L)
                .metodoPagamento(MetodoPagamento.PIX)
                .chaveIdempotencia(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post("/pagamentos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RECURSO_NAO_ENCONTRADO"));
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
