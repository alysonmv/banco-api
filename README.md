# Banco API

[![CI](https://github.com/alysonmv/banco-api/actions/workflows/ci.yml/badge.svg)](https://github.com/alysonmv/banco-api/actions/workflows/ci.yml)

API REST de um banco digital simplificado. A função central é transferência de valores entre
contas com atomicidade e correção sob alta concorrência: a transferência não pode perder nem
criar dinheiro sob requisições concorrentes. O resto (mensageria, observabilidade, docs) é
infraestrutura para sustentar essa garantia.

**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL · Flyway · RabbitMQ · springdoc-openapi ·
Spring Boot Actuator · JUnit 5 / Mockito / Testcontainers · Docker Compose.

---

## Como rodar

### Opção 1 — Tudo no Docker (recomendado)

Pré-requisito: Docker + Docker Compose.

```bash
docker compose up --build
```

Sobe app + postgres + rabbitmq. As migrations Flyway rodam no boot. Quando a app ficar
saudável:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
- RabbitMQ Management: `http://localhost:15672` (guest/guest)

As contas `1..4` já vêm semeadas (Alice, Isabella, Carol, João).

### Opção 2 — App local + dependências no Docker

```bash
# Sobe só postgres e rabbitmq
docker compose up -d postgres rabbitmq
# Roda a app pelo Maven
mvn spring-boot:run
```

### Rodar a suíte de testes

```bash
mvn verify
```

Executa testes unitários + integração com Testcontainers (Postgres e RabbitMQ reais, sem H2).
É necessário ter um Docker em execução. O `mvn verify` precisa terminar verde.

---

## Endpoints

| Método | Rota                          | Descrição                                   |
|--------|-------------------------------|---------------------------------------------|
| POST   | `/accounts`                   | Cadastra uma conta                          |
| GET    | `/accounts/{id}`              | Consulta conta/saldo                        |
| POST   | `/transfers`                  | Executa transferência (header `Idempotency-Key` opcional) |
| GET    | `/accounts/{id}/movements`    | Histórico paginado (`?page=0&size=20`)      |

Exemplos:

```bash
# Criar conta
curl -X POST http://localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"name":"Marcos","initialBalance":"500.00"}'

# Transferência idempotente
curl -X POST http://localhost:8080/transfers \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: 9f1c-abcd-0001' \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":"150.00"}'

# Saldo
curl http://localhost:8080/accounts/1

# Histórico
curl 'http://localhost:8080/accounts/1/movements?page=0&size=20'
```

Erros seguem um corpo consistente (`timestamp`, `status`, `code`, `message`, `correlationId`):
`404` conta inexistente, `422` saldo insuficiente, `400` valor inválido / mesma conta,
`409` replay de idempotência com payload divergente.

Todas as respostas incluem o header `X-Correlation-Id` (gerado se não enviado).

---

## Decisões de arquitetura

### Por que cada tecnologia da stack
- **PostgreSQL** (o teste deixava o banco livre): precisava de `SELECT ... FOR UPDATE` confiável e
  de um tipo numérico exato (`NUMERIC`) para dinheiro, ambos maduros no Postgres. Evitei H2 em
  runtime para não desenvolver/testar contra um banco diferente do de produção.
- **Flyway**: o schema fica em migrations versionadas e auditáveis, aplicadas no boot. Uso
  `ddl-auto=validate` (não `update`): o Hibernate valida o mapeamento, mas quem manda no schema é a
  migration; nada de schema "mágico" gerado em produção.
- **RabbitMQ**: a notificação precisa ser assíncrona e desacoplada da transação. Um broker dá
  processamento em outra thread/processo e entrega durável, sem amarrar o tempo de resposta da
  transferência ao envio. Um evento só em memória se perderia no restart e rodaria no mesmo processo.
- **Testcontainers**: os testes de integração rodam contra Postgres e RabbitMQ reais em
  containers efêmeros. Dá fidelidade ao runtime e acaba com o risco "passa no H2, quebra no Postgres"
  (lock pessimista e `ON CONFLICT` se comportam de forma específica do banco).
- **springdoc-openapi**: gera Swagger/OpenAPI a partir das anotações, sem manter uma spec à parte.
- **Docker Compose**: um único comando sobe app + Postgres + RabbitMQ, tornando o projeto
  reproduzível em qualquer máquina.

### Concorrência: lock pessimista + ordenação (vs. otimista)
A transferência adquire lock pessimista (`@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`)
nas duas contas, sempre na mesma ordem (id menor primeiro), valida o saldo dentro do lock e
só então debita/credita. A ordenação consistente dos locks elimina a espera circular, evitando
deadlock entre transferências A→B e B→A simultâneas. Escolhi pessimista em vez de otimista porque
o domínio é uma conta "quente" com escritas conflitantes frequentes: com lock otimista
(`@Version`), o conflito vira retry/falha sob contenção alta, exigindo política de retry e
piorando a latência justamente no caso quente. O pessimista serializa o acesso à conta de forma
previsível e correta, que é a prioridade aqui. O trade-off (gargalo na conta quente) está em
"Limitações".

### Dinheiro: `BigDecimal` com escala fixa
Todo valor monetário é `BigDecimal` com escala fixa de 2 casas e `RoundingMode.HALF_EVEN`
explícito, encapsulado no value object `Money` (domínio). Nunca `double`/`float`: ponto flutuante
binário não representa exatamente valores decimais (ex.: `0.1 + 0.2`), e em dinheiro esse erro de
arredondamento vira saldo errado. A coluna no banco é `NUMERIC(19,2)`.

### Idempotência
O endpoint de transferência aceita o header `Idempotency-Key`. A mesma chave retorna o
resultado original sem reexecutar; uma chave reutilizada com payload diferente retorna `409`.
A implementação é claim-first: dentro da transação, antes de transferir, inserimos a linha de
idempotência (`INSERT ... ON CONFLICT DO NOTHING`). A primary key serializa requests concorrentes
com a mesma chave: o segundo INSERT bloqueia na linha não-commitada e, após o commit do primeiro,
vira no-op; o perdedor então lê e devolve a resposta já persistida. Guardamos o hash do request
(detecta divergência) e a resposta serializada (replay fiel). Isso garante "exatamente uma"
execução mesmo sob duplo-submit concorrente, não só sequencial.

### Mensageria assíncrona pós-commit
Após o commit da transferência (`@TransactionalEventListener(AFTER_COMMIT)`), publicamos
`TransferCompletedEvent` numa fila RabbitMQ; um consumer recebe e simula o envio da notificação
ao cliente com um log estruturado. O pipeline (publicação durável, fila, consumo em outra thread) é
real; só o último passo, entregar num canal externo, é simulado. O ponto de extensão
para um envio real (e-mail/SMS/push) é o `TransferNotificationConsumer`: basta trocar o `log.info`
pela chamada ao serviço, mantendo todo o resto. Nunca publicamos dentro da transação de
débito/crédito. A notificação é não-crítica: se a publicação falhar, a transferência já foi
concluída e não é revertida, só logamos o erro (essa priorização está explícita em
`RabbitTransferPublisher`). O correlation id é propagado para a thread do consumer via header da
mensagem, pois o MDC não se propaga sozinho entre threads.

Limitação do publish pós-commit (sem outbox): entre o commit e o publish há uma janela em que,
se o processo cair, a notificação se perde (a transferência permanece íntegra). A solução robusta é
o Transactional Outbox (gravar o evento na mesma transação numa tabela `outbox` e um relay
publicar depois com retry). Ficou fora por tempo; está listado em próximos passos.

### Ledger imutável como fonte da verdade
As movimentações são gravadas numa tabela append-only (`movements`) e a consulta de histórico lê
dela: o histórico não é derivado do saldo. Um registro, uma vez gravado, nunca é alterado nem
apagado. Isso dá trilha de auditoria, permite reconstruir/reconciliar saldos a partir dos eventos e
elimina o risco de divergência entre "saldo atual" e "o que de fato aconteceu".

### Tratamento de erros consistente
Um único `@RestControllerAdvice` traduz tanto as exceções de domínio quanto as do framework para um
corpo de erro padrão (`timestamp, status, code, message, correlationId`) com o HTTP status correto
(`404/422/400/409/405/415`). O cliente sempre recebe o mesmo formato e um `code` estável e legível
por máquina, em vez de stack traces ou da página "Whitelabel" padrão do Spring.

### Clean Architecture (e onde fui pragmático)
- **domain** — Java puro, sem Spring/JPA: `Account`, `Movement`, `Money`, regras de débito/crédito
  e as interfaces de repositório (ports).
- **application** — casos de uso (`ExecuteTransferUseCase`, `GetMovementsUseCase`, `AccountUseCase`)
  que definem a transação e orquestram os ports.
- **infrastructure** — adapters: controllers REST, entidades JPA + mappers, repositórios com lock,
  publisher/consumer RabbitMQ, configs.

Onde a cerimônia não agregava, simplifiquei e registro aqui: a camada application usa
`@Transactional` e `ObjectMapper` diretamente (o domínio permanece 100% puro; só ele tem a regra
inegociável de não importar framework). Não criei interfaces/abstrações "para o futuro" sem um
segundo caso de uso real.

---

## Limitações e próximos passos
- **Conta quente é gargalo.** O lock pessimista serializa o acesso a uma conta muito disputada;
  o throughput por conta é limitado. Caminhos: particionar/saldar por agregação, filas por conta,
  ou um modelo de eventos (event sourcing) para o ledger.
- **Idempotência sem TTL cresce indefinidamente.** A tabela `idempotency_records` só cresce.
  Próximo passo: expiração/limpeza (job de purga) e/ou índice por `created_at`.
- **Sem outbox há janela de perda de notificação.** O publish pós-commit pode perder o evento se o
  processo cair entre o commit e o envio. Próximo passo: Transactional Outbox com relay e retry.
- **Sem autenticação/autorização** no core (fora de escopo). Em produção, proteger os endpoints.
- **Paginação simples** (`page`/`size`); para grandes volumes, considerar keyset pagination.

---

## Estrutura de arquivos

```
banco-api/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .github/workflows/ci.yml
├── src/main/java/com/banco/bancoapi/
│   ├── BancoApiApplication.java
│   ├── domain/                      # Java puro (sem Spring/JPA)
│   │   ├── model/                   # Money, Account, Movement, PagedResult, IdempotencyRecord
│   │   ├── exception/               # DomainException + específicas
│   │   └── port/                    # AccountRepository, MovementRepository, IdempotencyRepository
│   ├── application/                 # casos de uso (transação + orquestração)
│   │   ├── transfer/                # ExecuteTransferUseCase, Command/Result, TransferEventPublisher
│   │   ├── movement/                # GetMovementsUseCase
│   │   └── account/                 # AccountUseCase
│   └── infrastructure/
│       ├── persistence/             # entities JPA, mappers, adapters, repos com lock
│       ├── web/                     # controllers, DTOs, error handler, CorrelationIdFilter
│       ├── messaging/               # RabbitConfig, publisher pós-commit, consumer
│       └── config/                  # OpenApiConfig
└── src/main/resources/
    ├── application.yml
    └── db/migration/                # V1 accounts (+CHECK), V2 movements, V3 idempotency, V4 seed
```

---

## Testes

| Tipo            | Onde                                   | O que garante                                            |
|-----------------|----------------------------------------|----------------------------------------------------------|
| Unitário        | `MoneyTest`, `AccountTest`             | regras de dinheiro e débito/crédito                      |
| Unitário (use case) | `ExecuteTransferUseCaseTest` (Mockito) | ordenação de locks, validações, replay/conflito          |
| Integração      | `TransferFlowIT` (Testcontainers)      | fluxo real de transferência, saldos e ledger             |
| **Concorrência**| `ConcurrencyIT`                        | N transferências simultâneas: soma final = inicial, sem saldo negativo |
| **Deadlock**    | `DeadlockIT`                           | A→B e B→A em paralelo concluem sem deadlock, saldos corretos |
| **Idempotência**| `IdempotencyIT`                        | mesma chave não duplica (sequencial e concorrente); payload divergente → 409 |

Rode tudo com `mvn verify`.
