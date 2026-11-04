# Raízes do Nordeste - Backend

Sistema de gestão e pedidos multicanal para rede de culinária nordestina, integrando operações de balcão, totens, aplicativo, web e retirada com controle de estoque, fidelidade e simulação de pagamentos.

---

## 📋 Sumário
- [Visão Geral](#-visão-geral)
- [Funcionalidades Principais](#-funcionalidades-principais)
- [Arquitetura do Sistema](#-arquitetura-do-sistema)
- [Modelagem de Dados](#-modelagem-de-dados)
- [Requisitos Não Funcionais](#-requisitos-não-funcionais)
- [Estrutura do Repositório](#-estrutura-do-repositório)
- [Como Contribuir](#-como-contribuir)

---

## 🍲 Visão Geral
O **Raízes do Nordeste** é uma plataforma multicanal projetada para gerenciar o fluxo completo de atendimento de restaurantes, desde a consulta do cardápio e gestão de estoque local até o processamento de pedidos em tempo real para a cozinha e entrega.

---

## 🚀 Funcionalidades Principais
- **Autenticação e Perfis (RBAC):** Controle de permissões para Administrador, Gerente, Atendente e Cliente.
- **Multicanalidade de Pedidos:** Rastreamento do canal de entrada do pedido (`APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB`).
- **Gestão de Unidades e Cardápio:** Consulta dinâmica de produtos disponíveis por unidade.
- **Controle Rigoroso de Estoque:** Validação automática de disponibilidade de insumos/produtos com bloqueio de vendas indisponíveis.
- **Esteira de Pedidos (Cozinha e Atendimento):** Ciclo de vida: `AGUARDANDO_PAGAMENTO` ➔ `PREPARACAO` ➔ `PRONTO` ➔ `ENTREGUE` (ou `CANCELADO`).
- **Simulação de Pagamento (Mock):** Gateway simulado com aprovação/recusa e atualização de status em tempo real.
- **Fidelização e Descontos:** Acúmulo e resgate de pontos mediante consentimento do usuário.

---

## 🏛️ Arquitetura do Sistema
O projeto segue o modelo de arquitetura em camadas:

```
├── Domain/           # Entidades centrais, Enums e regras de negócio essenciais
├── Application/      # Casos de uso, orquestração e DTOs
├── Infrastructure/   # Persistência de dados, ORM, clientes HTTP e integrações externas
└── API/              # Controllers, middlewares, autenticação JWT e documentação Swagger/OpenAPI
```

---

## 🗄️ Modelagem de Dados
Entidades principais mapeadas:
- **Clientes / Usuários:** Identificação, credenciais seguras, papéis e consentimento LGPD.
- **Unidades:** Lojas físicas e operações ativas.
- **Produtos:** Itens do cardápio e preços base.
- **Estoque:** Disponibilidade de itens vinculados a cada unidade.
- **Pedidos:** Rastreio do cliente, unidade, canal de atendimento, status e valor total.
- **Itens de Pedido:** Itens adquiridos com preço unitário garantido no momento da compra.
- **Pagamento (Mock):** Registro das transações e métodos de pagamento.

Consulte a especificação completa no arquivo [`Modelagem e Requisitos - Raízes do Nordeste.md`](./Modelagem%20e%20Requisitos%20-%20Raízes%20do%20Nordeste.md).

---

## 🛡️ Requisitos Não Funcionais
- **Segurança & LGPD:** Criptografia de senhas (hashing), tokens JWT, sanitização de dados sensíveis e conformidade legal.
- **Auditoria e Logs:** Rastreabilidade de alterações de pedidos e estoques.
- **Resiliência:** Tratamento de erros e tolerância a falhas na integração de pagamentos.
- **Documentação de API:** Contratos padronizados via OpenAPI / Swagger.

---

## 💻 Como Iniciar o Desenvolvimento
1. Clone o repositório:
   ```bash
   git clone https://github.com/silasogis/projeto-raizes-do-nordeste.git
   ```
2. Crie uma branch para a sua feature:
   ```bash
   git checkout -b feature/nome-da-feature
   ```
3. Realize seus commits seguindo boas práticas e abra seu Pull Request!
