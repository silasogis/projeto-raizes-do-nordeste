-- ========================================================
-- V2__seed_data.sql
-- Projeto: Raízes do Nordeste Back-End
-- Dados Iniciais de Seed (Usuários, Unidades, Cardápio e Estoque)
-- Hash BCrypt padrão para todos os usuários de teste: Senha@123
-- ========================================================

-- 1. Inserção de Usuários para todos os perfis do sistema
-- Senha de todos: Senha@123 (BCrypt hash)
INSERT INTO usuarios (id, nome, email, senha_hash, perfil, pontos_fidelidade, consentimento_lgpd, data_criacao)
VALUES
    (1, 'Administrador do Sistema', 'admin@raizesdonordeste.com.br', '$2a$10$7Q9U8h6zQfT9qP.V7wX6xeUvHfZqC2B9aO7bN8b8K6ZgqU4Jp3y7a', 'ADMIN', 0, TRUE, CURRENT_TIMESTAMP),
    (2, 'Carlos Gerente Recife', 'gerente.recife@raizesdonordeste.com.br', '$2a$10$7Q9U8h6zQfT9qP.V7wX6xeUvHfZqC2B9aO7bN8b8K6ZgqU4Jp3y7a', 'GERENTE', 0, TRUE, CURRENT_TIMESTAMP),
    (3, 'Ana Atendente Balcão', 'atendente.recife@raizesdonordeste.com.br', '$2a$10$7Q9U8h6zQfT9qP.V7wX6xeUvHfZqC2B9aO7bN8b8K6ZgqU4Jp3y7a', 'ATENDENTE', 0, TRUE, CURRENT_TIMESTAMP),
    (4, 'Severino Cozinheiro Chefe', 'cozinha.recife@raizesdonordeste.com.br', '$2a$10$7Q9U8h6zQfT9qP.V7wX6xeUvHfZqC2B9aO7bN8b8K6ZgqU4Jp3y7a', 'COZINHA', 0, TRUE, CURRENT_TIMESTAMP),
    (5, 'João Silva Santos', 'cliente.joao@gmail.com', '$2a$10$7Q9U8h6zQfT9qP.V7wX6xeUvHfZqC2B9aO7bN8b8K6ZgqU4Jp3y7a', 'CLIENTE', 50, TRUE, CURRENT_TIMESTAMP),
    (6, 'Maria Oliveira Souza', 'cliente.maria@gmail.com', '$2a$10$7Q9U8h6zQfT9qP.V7wX6xeUvHfZqC2B9aO7bN8b8K6ZgqU4Jp3y7a', 'CLIENTE', 120, TRUE, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE nome = VALUES(nome);

-- 2. Inserção de Unidades da Rede
INSERT INTO unidades (id, nome, endereco, ativa, data_criacao)
VALUES
    (1, 'Raízes do Nordeste - Recife Matriz', 'Rua do Bom Jesus, 120 - Bairro do Recife, Recife - PE', TRUE, CURRENT_TIMESTAMP),
    (2, 'Raízes do Nordeste - Salvador Pelourinho', 'Largo do Pelourinho, 45 - Pelourinho, Salvador - BA', TRUE, CURRENT_TIMESTAMP),
    (3, 'Raízes do Nordeste - Fortaleza Beira Mar', 'Av. Beira Mar, 2800 - Meireles, Fortaleza - CE', TRUE, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE nome = VALUES(nome);

-- 3. Inserção de Produtos Típicos no Cardápio
INSERT INTO produtos (id, nome, descricao, preco_base, ativo, data_criacao)
VALUES
    (1, 'Baião de Dois Especial', 'Arroz, feijão de corda, queijo coalho tostado, carne de sol desfiada, bacon e manteiga de garrafa.', 38.50, TRUE, CURRENT_TIMESTAMP),
    (2, 'Carne de Sol com Macaxeira', 'Carne de sol artesanal frita na manteiga de garrafa acompanhada de macaxeira cozida e pirão de queijo.', 49.90, TRUE, CURRENT_TIMESTAMP),
    (3, 'Moqueca Baiana Tradicional', 'Peixe fresco cozido em azeite de dendê, leite de coco natural, pimentões, cebola, tomate e coentro.', 65.00, TRUE, CURRENT_TIMESTAMP),
    (4, 'Cuscuz Nordestino Completo', 'Cuscuz de milho fofinho com carne seca acebolada, ovos caipiras e queijo coalho derretido.', 18.00, TRUE, CURRENT_TIMESTAMP),
    (5, 'Acarajé Tradicional 2 unid.', 'Bolinhos de feijão fradinho fritos no azeite de dendê recheados com vatapá, caruru e camarão seco.', 22.00, TRUE, CURRENT_TIMESTAMP),
    (6, 'Cartola Pernambucana', 'Sobremesa de banana frita na manteiga, coberta com queijo manteiga assado, açúcar e canela.', 16.50, TRUE, CURRENT_TIMESTAMP),
    (7, 'Suco Natural de Caju 500ml', 'Suco natural da fruta caju fresco servido bem gelado.', 9.00, TRUE, CURRENT_TIMESTAMP),
    (8, 'Refrigerante Guaraná Jesus Lata 350ml', 'Refrigerante gaseificado doce típico maranhense sabor cravo e canela.', 7.50, TRUE, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE nome = VALUES(nome);

-- 4. Inserção de Saldo de Estoque por Unidade
-- Unidade 1 (Recife)
INSERT INTO estoque (unidade_id, produto_id, quantidade_disponivel, data_atualizacao)
VALUES
    (1, 1, 50, CURRENT_TIMESTAMP),
    (1, 2, 40, CURRENT_TIMESTAMP),
    (1, 3, 30, CURRENT_TIMESTAMP),
    (1, 4, 60, CURRENT_TIMESTAMP),
    (1, 5, 25, CURRENT_TIMESTAMP),
    (1, 6, 45, CURRENT_TIMESTAMP),
    (1, 7, 80, CURRENT_TIMESTAMP),
    (1, 8, 100, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE quantidade_disponivel = VALUES(quantidade_disponivel);

-- Unidade 2 (Salvador)
INSERT INTO estoque (unidade_id, produto_id, quantidade_disponivel, data_atualizacao)
VALUES
    (2, 1, 35, CURRENT_TIMESTAMP),
    (2, 2, 30, CURRENT_TIMESTAMP),
    (2, 3, 60, CURRENT_TIMESTAMP),
    (2, 4, 40, CURRENT_TIMESTAMP),
    (2, 5, 70, CURRENT_TIMESTAMP),
    (2, 6, 30, CURRENT_TIMESTAMP),
    (2, 7, 50, CURRENT_TIMESTAMP),
    (2, 8, 60, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE quantidade_disponivel = VALUES(quantidade_disponivel);

-- Unidade 3 (Fortaleza)
INSERT INTO estoque (unidade_id, produto_id, quantidade_disponivel, data_atualizacao)
VALUES
    (3, 1, 45, CURRENT_TIMESTAMP),
    (3, 2, 50, CURRENT_TIMESTAMP),
    (3, 3, 25, CURRENT_TIMESTAMP),
    (3, 4, 50, CURRENT_TIMESTAMP),
    (3, 5, 30, CURRENT_TIMESTAMP),
    (3, 6, 35, CURRENT_TIMESTAMP),
    (3, 7, 70, CURRENT_TIMESTAMP),
    (3, 8, 80, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE quantidade_disponivel = VALUES(quantidade_disponivel);
