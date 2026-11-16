# Plano de Desenvolvimento (MVP Enxuto) — Raízes do Nordeste Back-End

Guia progressivo de execução estritamente alinhado aos requisitos e critérios oficiais de avaliação dos documentos [`Projeto Multidisciplinar - Estudo de Caso rev 0.3_final.pdf`](./Projeto%20Multidisciplinar%20-%20Estudo%20de%20Caso%20rev%200.3_final.pdf) e [`Roteiro de Atividade Pratica - 2026 - Projeto Back End.pdf`](./Roteiro%20de%20Atividade%20Pratica%20-%202026%20-%20Projeto%20Back%20End.pdf).

---

## Diretriz de Escopo (Sem Excessos)

* **Stack:** Java 17 + Spring Boot + Maven (`mvnw`) + Spring Web + Spring Data JPA + Spring Security (JWT) + Bean Validation + Flyway + MySQL + `springdoc-openapi` (Swagger).
* **Arquitetura em Camadas:** `api` (controllers/DTOs/handlers) ➔ `application` (services/casos de uso) ➔ `domain` (entidades/enums/regras) ➔ `infrastructure` (repositories/security/config/migrations).
* **Fluxo Crítico Escolhido (MVP Completo):** **Fluxo A — Pedido ➔ Pagamento Mock ➔ Atualização de Status** (com multicanalidade `canalPedido`, estoque por unidade, RBAC, LGPD e auditoria).
* **O que NÃO codificar (apenas documentar como evolução planejada):** motor complexo de promoções/campanhas segmentadas, dashboards analíticos da matriz e gateway de pagamento real.

---

## Fase 0 — Alinhamento da Modelagem e Contratos
**Objetivo:** Garantir 100% de coerência entre modelagem, banco de dados e endpoints antes de escrever código.

* [x] Fechar a lista de perfis (`ENUM`: `ADMIN`, `GERENTE`, `ATENDENTE`, `COZINHA`, `CLIENTE`).
* [x] Alinhar o DER para incluir explicitamente as tabelas necessárias ao fluxo: `usuarios`, `unidades`, `produtos`, `estoque` (`unidade_id`, `produto_id` únicos), `pedidos` (com `canal_pedido`), `itens_pedido`, `pagamentos` (mock, status, payload, chave de idempotência) e `auditoria_logs`.
* [x] Definir os contratos mínimos dos endpoints (URLs no plural, paginação `?page=&limit=`, status codes e JSON padrão de erro com `{ error, message, details, timestamp, path, requestId }`).
* [x] Definir o que entra no **MVP implementado** vs. **Proposto/Planejado** para justificar na documentação.

**Critério de pronto:** Modelagem revisada sem divergências em relação aos PDFs. *(Fase 0 Concluída)*

---

## Fase 1 — Setup do Projeto e Infraestrutura Base
**Objetivo:** Ter a aplicação Spring Boot subindo localmente com banco versionado, Swagger ativo e padrão global de erros.

* [x] Inicializar projeto Spring Boot com Maven Wrapper (`./mvnw`) e estrutura de pacotes em camadas (`api`, `application`, `domain`, `infrastructure`).
* [x] Criar `docker-compose.yml` (MySQL) e `.env.example` (credenciais do banco e segredo JWT via variáveis de ambiente, sem senhas hardcoded).
* [x] Configurar Flyway com migration inicial (`V1__create_schema.sql`) fiel ao DER e migration de seed (`V2__seed_data.sql`) com usuários iniciais, unidades, produtos e saldos de estoque.
* [x] Configurar `springdoc-openapi` (`/swagger-ui.html`) com suporte a Bearer Token (JWT).
* [x] Implementar `@RestControllerAdvice` (`GlobalExceptionHandler`) retornando o JSON padrão de erro exigido para `400/422` (validação Bean Validation), `404`, `409` e `500`.

**Critério de pronto:** API inicia com `./mvnw spring-boot:run`, Flyway cria tabelas + seed no MySQL e Swagger abre sem erros. *(Fase 1 Concluída - Commit 1)*

---

## Fase 2 — Autenticação, Perfis (RBAC) e LGPD
**Objetivo:** Garantir acesso autenticado por JWT, controle de permissões por perfil e proteção de dados pessoais.

* [ ] Implementar entidade/repositório de `Usuario` (com `senhaHash` usando `BCryptPasswordEncoder`, `perfil` e `consentimentoLgpd` obrigatório no cadastro).
* [ ] Implementar filtro JWT stateless + `AuthenticationEntryPoint` (`401`) e `AccessDeniedHandler` (`403`) devolvendo o JSON padrão de erro.
* [ ] Criar endpoints públicos de autenticação:
  * `POST /auth/register` (cadastro de cliente validando `consentimentoLgpd = true`, retornando DTO sem `senhaHash`).
  * `POST /auth/login` (retorna `accessToken`, `tokenType`, `expiresIn` e resumo mínimo do usuário).
* [ ] Configurar regras de autorização por role (`@PreAuthorize` / SecurityFilterChain).

**Critério de pronto:** Login retorna JWT válido; rotas protegidas retornam `401` sem token e `403` quando o perfil não tem permissão. *(Commit 2)*

---

## Fase 3 — Unidades, Cardápio e Estoque por Unidade
**Objetivo:** Permitir consultar lojas, cardápio disponível na unidade e gerenciar saldo de estoque.

* [ ] Implementar `Unidade`, `Produto` e `Estoque` (chave única `unidade_id + produto_id` e validação `quantidadeDisponivel >= 0`).
* [ ] Criar endpoints essenciais:
  * `GET /unidades` (listagem paginada).
  * `GET /unidades/{id}/produtos` (cardápio disponível naquela unidade; opcional `GET /produtos` paginado).
  * `POST /produtos` (restrito a `ADMIN` / `GERENTE`).
  * `GET /estoque?unidadeId=...` e `PATCH /estoque` (entrada/saída de estoque restrito a `GERENTE` / `ADMIN`, registrando auditoria).

**Critério de pronto:** É possível consultar unidades, produtos da unidade e atualizar estoque via Swagger/Postman. *(Commit 3)*

---

## Fase 4 — Fluxo Crítico: Pedido + Pagamento Mock + Status + Auditoria
**Objetivo:** Fechar de ponta a ponta o fluxo principal exigido no roteiro com atomicidade e rastreabilidade.

* [ ] Implementar `Pedido` (com `canalPedido` obrigatório: `APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB` e `status`: `AGUARDANDO_PAGAMENTO`, `PREPARACAO`, `PRONTO`, `ENTREGUE`, `CANCELADO`) e `ItemPedido` (`precoUnitarioGarantido`).
* [ ] Implementar `POST /pedidos` (`@Transactional`):
  * Validar `canalPedido` (`400/422` se ausente/inválido), existência da unidade e produtos (`404`).
  * Validar disponibilidade no estoque da unidade e realizar a baixa; se faltar saldo, abortar com `409 Conflict` (`ESTOQUE_INSUFICIENTE` detalhando o item).
  * Calcular `valorTotal` no servidor, salvar pedido em `AGUARDANDO_PAGAMENTO` e gravar log de auditoria.
* [ ] Implementar `GET /pedidos` com paginação e filtro obrigatório por canal e status (`?canalPedido=TOTEM&status=...`) e `GET /pedidos/{id}` (respeitando visibilidade por perfil).
* [ ] Implementar `POST /pagamentos` (Gateway Mock desacoplado):
  * Suportar chave de idempotência para evitar cobrança duplicada.
  * Simular retorno **aprovado** (atualiza pedido para `PREPARACAO`) e **recusado/falha** (registra retorno/payload do mock, cancela ou mantém falha coerente e estorna estoque se cancelado).
* [ ] Implementar `PATCH /pedidos/{id}/status`:
  * Permitir transições válidas da esteira (`PREPARACAO ➔ PRONTO ➔ ENTREGUE` ou `CANCELADO`) apenas para perfis autorizados (`COZINHA`, `ATENDENTE`, `GERENTE`).
  * Gravar registro na tabela `auditoria_logs` (usuário/ator, ação, entidade, id, timestamp e resumo) em criação, pagamento, mudança de status e cancelamento; opcionalmente acumular `pontosFidelidade` simples na conclusão se o cliente tiver `consentimentoLgpd = true`.

**Critério de pronto:** Fluxo completo `Criar Pedido ➔ Validar Estoque ➔ Pagar (Mock Aprovado/Recusado) ➔ Avançar Status ➔ Auditar` funcionando com persistência real. *(Commit 4)*

---

## Fase 5 — Plano de Testes, Coleção Executável e README
**Objetivo:** Garantir que o corretor consiga executar e validar tudo sem suposições.

* [ ] Criar coleção **Postman ou Insomnia** (`.json`) no repositório organizada em pastas (`Auth`, `Unidades e Estoque`, `Pedidos`, `Pagamentos`, `Erros`) refletindo **no mínimo 10 cenários** (≥ 6 positivos, ≥ 4 negativos):
  1. Login válido (`200`)
  2. Acesso sem token (`401`)
  3. Acesso com perfil sem permissão (`403`)
  4. Criação de pedido sem campo obrigatório / `canalPedido` ausente (`400/422`)
  5. Validação de formato/quantidade negativa (`400/422`)
  6. Consulta de unidades / cardápio por unidade (`200`)
  7. Pedido válido criado (`201`) + consulta filtrada por `?canalPedido=` (`200`)
  8. Pedido com produto ou unidade inexistente (`404`)
  9. Pedido com estoque insuficiente (`409`)
  10. Pagamento mock aprovado (`PREPARACAO`) + atualização de status (`PRONTO` / `ENTREGUE`)
  11. Pagamento mock recusado (status e resposta coerentes)
  12. Evidência de registro de auditoria para ação sensível (ex.: consulta/listagem de auditoria por `ADMIN`/`GERENTE` ou evidência no fluxo)
* [ ] Escrever testes automatizados básicos em JUnit/MockMvc (ou validar todos via coleção).
* [ ] Atualizar o [`README.md`](./README.md) com guia exato de execução: requisitos, `.env`, subida do MySQL via Docker, execução da API, URL/rota do Swagger, ordem de execução dos testes e credenciais de seed.

**Critério de pronto:** Qualquer pessoa clonando o repositório público consegue rodar a API e executar a coleção de testes do zero seguindo apenas o `README.md`. *(Commit 5+)*

---

## Fase 6 — Documento Final (PDF) e Checklist de Entrega
**Objetivo:** Produzir o entregável único exigido pela disciplina seguindo normas da ABNT e tabela de critérios.

* [ ] Montar o documento único `<RU>_Projeto_Back_End.PDF` com:
  1. Capa e Sumário (ABNT)
  2. Introdução e Objetivos
  3. Análise e Requisitos (RF, RNF, Multicanalidade, Priorização MVP × Planejado)
  4. Modelagem e Arquitetura (Casos de Uso + descrição da feature crítica, DER atualizado, Diagrama de Classes, Diagrama de Sequência/Atividade do fluxo crítico, Arquitetura em Camadas)
  5. API e Endpoints (documentação/prints do Swagger + padrão JSON de erro)
  6. LGPD, Privacidade, Segurança e Auditoria (finalidade, base legal, minimização, hash, roles, logs)
  7. Entrega Técnica (links **públicos** para o GitHub, Swagger local/instruções e coleção Postman/Insomnia)
  8. Plano de Testes (tabela com ID, cenário, endpoint, pré-condição, entrada, esperado e evidência)
  9. Conclusão, Referências e Declaração de uso de IA (se aplicável).
* [ ] Conferir se o repositório no GitHub está **público** e com os arquivos finais sincronizados (`git push`).
