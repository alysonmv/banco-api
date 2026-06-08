-- Registro de idempotencia: mesma Idempotency-Key retorna o resultado original
-- sem reexecutar a transferencia. request_hash detecta replay com payload divergente (409).
--
-- Estrategia "claim-first" para seguranca sob concorrencia: a linha e inserida (claim)
-- ANTES de executar a transferencia, dentro da mesma transacao. A PK serializa requests
-- concorrentes com a mesma chave (o segundo INSERT bloqueia no row nao-commitado e, apos
-- o commit do primeiro, vira no-op). Por isso movement_id/response sao preenchidos depois
-- e ficam nulos ate a conclusao — uma linha commitada sempre tem esses campos preenchidos.
CREATE TABLE idempotency_records (
    idempotency_key  VARCHAR(120) PRIMARY KEY,
    request_hash     VARCHAR(64)  NOT NULL,
    movement_id      BIGINT       REFERENCES movements (id),
    response_status  INT,
    response_body    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
