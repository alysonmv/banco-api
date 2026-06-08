-- Base pre-carregada de contas para uso imediato da API.
INSERT INTO accounts (id, name, balance) VALUES
    (1, 'Alice',    1000.00),
    (2, 'Isabella', 1000.00),
    (3, 'Carol',     500.00),
    (4, 'João',        0.00);

-- Avanca a sequence de identidade para alem dos ids semeados manualmente.
SELECT setval(pg_get_serial_sequence('accounts', 'id'), (SELECT MAX(id) FROM accounts));
