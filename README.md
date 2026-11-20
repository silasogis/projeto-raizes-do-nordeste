# Projeto: Desenvolvimento Back-end (929602)

**Aluno:** Silas de Oliveira Nascimento  
**RU:** 1572541  
**Repositório:** [https://github.com/silasogis/projeto-raizes-do-nordeste](https://github.com/silasogis/projeto-raizes-do-nordeste)

---

## 1. Visão Geral da Solução

API REST desenvolvida para a rede de restaurantes Raízes do Nordeste, implementando o fluxo crítico completo de atendimento multicanal (APP, TOTEM, BALCAO, PICKUP, WEB):
- Cadastro e autenticação com JWT e consentimento LGPD;
- Controle de acesso baseado em papéis (RBAC);
- Gestão de unidades e estoque local com validação atômica de disponibilidade;
- Criação e acompanhamento de pedidos multicanal;
- Simulação desacoplada de pagamento (Gateway Mock) com idempotência e estorno;
- Esteira de status do pedido e trilha de auditoria para ações críticas.

**Stack Tecnológica:** Java 17, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Hibernate, MySQL 8.0, Flyway Migration, Bean Validation, springdoc-openapi (Swagger UI) e JUnit 5 / MockMvc.

---

## 2. Links e Evidências

- **Repositório GitHub:** [https://github.com/silasogis/projeto-raizes-do-nordeste](https://github.com/silasogis/projeto-raizes-do-nordeste)
- **Documentação Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Docs:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Coleção Postman:** Arquivo `postman_collection.json` disponível na raiz do repositório.

---

## 3. Pré-requisitos de Ambiente

- **Java JDK:** versão 17 ou superior instalada.
- **Docker e Docker Compose:** instalados e em execução.
- **Maven:** dispensável instalação manual prévia (o projeto disponibiliza o wrapper `./mvnw`).

---

## 4. Instruções de Execução

### Passo 1: Clonar o Repositório
```bash
git clone https://github.com/silasogis/projeto-raizes-do-nordeste.git
cd projeto-raizes-do-nordeste
```

### Passo 2: Configurar as Variáveis de Ambiente
Copie o arquivo de exemplo para criar o `.env` local:
```bash
cp .env.example .env
```
O arquivo `.env.example` já vem configurado com valores padrão compatíveis com o ambiente de desenvolvimento local e com o container Docker do MySQL.

### Passo 3: Iniciar o Banco de Dados MySQL via Docker
Suba a instância do MySQL 8.0 em segundo plano:
```bash
docker compose up -d
```
Aguarde cerca de 10 segundos até o container estar pronto para receber conexões na porta 3306.

### Passo 4: Executar a Aplicação Spring Boot
Inicie a aplicação utilizando o Maven Wrapper:
```bash
./mvnw spring-boot:run
```
O Flyway executará automaticamente a criação de todas as tabelas (`V1__create_schema.sql`) e a inserção dos dados iniciais de seed (`V2__seed_data.sql`).

A API estará acessível em `http://localhost:8080`.

---

## 5. Credenciais de Teste (Carga Inicial de Seed)

A senha padrão para todos os usuários criados no seed é: `Senha@123`

| Perfil | Nome | E-mail | Senha |
|---|---|---|---|
| ADMIN | Administrador do Sistema | admin@raizesdonordeste.com.br | Senha@123 |
| GERENTE | Carlos Gerente Recife | gerente.recife@raizesdonordeste.com.br | Senha@123 |
| ATENDENTE | Ana Atendente Balcão | atendente.recife@raizesdonordeste.com.br | Senha@123 |
| COZINHA | Severino Cozinheiro Chefe | cozinha.recife@raizesdonordeste.com.br | Senha@123 |
| CLIENTE | João Silva Santos | cliente.joao@gmail.com | Senha@123 |
| CLIENTE | Maria Oliveira Souza | cliente.maria@gmail.com | Senha@123 |

---

## 6. Execução dos Testes

### 6.1 Testes Automatizados (JUnit 5 / MockMvc)
Para rodar a suíte completa de testes de integração e validação de handlers com banco em memória (H2):
```bash
./mvnw test
```
Todos os testes são executados de forma isolada, cobrindo autenticação, autorização por perfil, criação de pedidos, regras de estoque, idempotência de pagamento e tratamento padronizado de exceções.

### 6.2 Coleção Postman (`postman_collection.json`)
O arquivo `postman_collection.json` na raiz do projeto está estruturado em pastas organizadas por domínio. As requisições contêm testes automáticos que capturam tokens e IDs dinamicamente.

**Ordem recomendada de execução:**
1. **1. Auth:** Executar `T01 - Login Cliente`, `T02 - Login Administrador` e `T03 - Login Cozinha` (os tokens JWT são salvos automaticamente nas variáveis da coleção).
2. **2. Unidades e Estoque:** Executar listagens de lojas, cardápio e verificação de saldos em estoque.
3. **3. Pedidos:** Executar `T09 - Criar Pedido Válido Multicanal TOTEM` (o ID retornado é capturado em `{{pedidoId}}`) e `T10 - Criar Segundo Pedido` (para teste de recusa).
4. **4. Pagamentos (Mock):** Executar `T15 - Processar Pagamento Mock Aprovado` (o pedido avança para `PREPARACAO`) e `T17 - Processar Pagamento Mock Recusado` (o pedido é cancelado e o estoque é estornado).
5. **3. Pedidos (Esteira):** Executar `T13 - Atualizar Status para PRONTO` e `T14 - Atualizar Status para ENTREGUE`.
6. **5. Auditoria:** Executar `T18 - Consultar Logs de Auditoria` para verificar o registro das ações sensíveis realizadas.
7. **6. Erros e Validações:** Executar as requisições negativas (T19 a T25) para confirmar os status codes de erro padronizados (`401`, `403`, `404`, `409` e `422`).

---

## 7. Plano de Testes (Mínimo de Cenários Conforme Roteiro)

| ID | Cenário | Método e Endpoint | Pré-condição | Entrada (Body / Parâmetros) | Resultado Esperado | Evidência na Coleção Postman |
|---|---|---|---|---|---|---|
| T01 | Login válido com emissão de token JWT | POST /auth/login | Usuário cadastrado no seed | `{"email": "cliente.joao@gmail.com", "senha": "Senha@123"}` | 200 OK, token JWT retornado | `1. Auth / T01 - Login Cliente (200)` |
| T02 | Login de administrador para operações restritas | POST /auth/login | Admin cadastrado no seed | `{"email": "admin@raizesdonordeste.com.br", "senha": "Senha@123"}` | 200 OK, token com perfil ADMIN | `1. Auth / T02 - Login Administrador (200)` |
| T03 | Consulta do perfil autenticado pelo token | GET /auth/me | Header Authorization com Bearer Token | Nenhum body | 200 OK, dados do cliente e consentimento LGPD | `1. Auth / T04 - Consultar Perfil Autenticado /me (200)` |
| T04 | Cadastro de novo cliente com consentimento LGPD | POST /auth/register | E-mail não cadastrado | `{"nome": "...", "email": "...", "senha": "...", "consentimentoLgpd": true}` | 201 Created, DTO sem senha | `1. Auth / T05 - Cadastro de Novo Cliente com LGPD (201)` |
| T05 | Consulta paginada de unidades ativas da rede | GET /unidades | Usuário autenticado | Query params: `?page=0&limit=10` | 200 OK, lista paginada de unidades | `2. Unidades e Estoque / T06 - Listar Unidades da Rede (200)` |
| T06 | Consulta de cardápio com saldo de estoque da loja | GET /unidades/1/produtos | Unidade 1 existente e ativa | Query params: `?page=0&limit=10` | 200 OK, produtos e saldos disponíveis | `2. Unidades e Estoque / T07 - Consultar Cardápio por Unidade (200)` |
| T07 | Criação de pedido multicanal com baixa de estoque | POST /pedidos | Unidade 1 ativa e estoque suficiente | `{"unidadeId": 1, "canalPedido": "TOTEM", "itens": [{"produtoId": 1, "quantidade": 1}]}` | 201 Created, status AGUARDANDO_PAGAMENTO | `3. Pedidos / T09 - Criar Pedido Válido Multicanal TOTEM (201)` |
| T08 | Consulta de pedidos filtrados por canal de origem | GET /pedidos | Pedidos cadastrados no canal | Query params: `?canalPedido=TOTEM&page=0&limit=10` | 200 OK, apenas pedidos com canal TOTEM | `3. Pedidos / T12 - Listar Pedidos Filtrados por Canal TOTEM (200)` |
| T09 | Pagamento mock aprovado com avanço na esteira | POST /pagamentos | Pedido em AGUARDANDO_PAGAMENTO | `{"pedidoId": 1, "metodoPagamento": "PIX", "chaveIdempotencia": "uuid", "simularStatus": "APROVADO"}` | 201 Created, pedido atualizado para PREPARACAO | `4. Pagamentos (Mock) / T15 - Processar Pagamento Mock Aprovado (201)` |
| T10 | Pagamento mock recusado com cancelamento e estorno | POST /pagamentos | Segundo pedido aguardando pagamento | `{"pedidoId": 2, "metodoPagamento": "CARTAO_CREDITO", "chaveIdempotencia": "uuid", "simularStatus": "RECUSADO"}` | 201 Created, status RECUSADO, pedido CANCELADO e estoque estornado | `4. Pagamentos (Mock) / T17 - Processar Pagamento Mock Recusado com Estorno (201)` |
| T11 | Atualização de status da esteira (PRONTO e ENTREGUE) | PATCH /pedidos/1/status | Perfil COZINHA/ADMIN, pedido pago | `{"novoStatus": "PRONTO"}` | 200 OK, status alterado para PRONTO | `3. Pedidos / T13 - Atualizar Status do Pedido para PRONTO (200)` |
| T12 | Verificação de registro de auditoria para ação crítica | GET /auditoria | Ação de pedido/pagamento executada, perfil ADMIN | Query params: `?entidade=PEDIDO&page=0&limit=10` | 200 OK, logs com usuário, ação e timestamp | `5. Auditoria / T18 - Consultar Logs de Auditoria para Ações Sensíveis (200)` |
| T13 | Acesso a recurso protegido sem token JWT | GET /pedidos | Nenhuma autenticação enviada | Nenhum body | 401 Unauthorized, JSON padronizado | `6. Erros e Validações / T19 - Acesso sem Token JWT (401)` |
| T14 | Acesso a endpoint administrativo por perfil CLIENTE | GET /auth/admin-check | Token de usuário com perfil CLIENTE | Nenhum body | 403 Forbidden, JSON padronizado | `6. Erros e Validações / T20 - Acesso com Perfil sem Permissão (403)` |
| T15 | Criação de pedido com campo obrigatório ausente | POST /pedidos | Cliente autenticado | `{"unidadeId": 1, "itens": [...]}` (sem canalPedido) | 422 Unprocessable Entity, detalhe do campo | `6. Erros e Validações / T21 - Criar Pedido sem Canal Obrigatório (422)` |
| T16 | Criação de pedido com formato/quantidade inválida | POST /pedidos | Cliente autenticado | `{"unidadeId": 1, "canalPedido": "BALCAO", "itens": [{"produtoId": 1, "quantidade": -5}]}` | 422 Unprocessable Entity, detalhe de validação | `6. Erros e Validações / T22 - Criar Pedido com Quantidade Negativa ou Inválida (422)` |
| T17 | Criação de pedido para unidade inexistente | POST /pedidos | Cliente autenticado | `{"unidadeId": 9999, "canalPedido": "APP", "itens": [{"produtoId": 1, "quantidade": 1}]}` | 404 Not Found, mensagem descritiva | `6. Erros e Validações / T23 - Criar Pedido com Unidade Inexistente (404)` |
| T18 | Criação de pedido com estoque insuficiente | POST /pedidos | Cliente autenticado | `{"unidadeId": 1, "canalPedido": "WEB", "itens": [{"produtoId": 1, "quantidade": 999999}]}` | 409 Conflict, mensagem de falta de saldo | `6. Erros e Validações / T24 - Criar Pedido com Estoque Insuficiente (409)` |
| T19 | Tentativa de pagamento para pedido já finalizado | POST /pagamentos | Pedido já processado/pago anteriormente | `{"pedidoId": 1, "metodoPagamento": "PIX", "chaveIdempotencia": "uuid-duplicado"}` | 409 Conflict, recusa de duplicidade | `6. Erros e Validações / T25 - Pagamento com Pedido Já Finalizado (409)` |
