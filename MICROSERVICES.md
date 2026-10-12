# GalileoAPI microservices readiness guide

GalileoAPI is intentionally a **modular monolith today**. That is the safest place to learn transactions, domain rules, and module boundaries. It is microservices-ready in the practical sense: features are grouped by business capability, deployment configuration is externalized, the process is containerized, requests carry correlation IDs, metrics are exportable, and Redis is treated as disposable infrastructure rather than the source of truth.

“Microservices-ready” does not mean that copying each package into a new application is safe. A service split changes transaction, data ownership, latency, consistency, security, deployment, and operations. Split only when independent ownership or scaling justifies that cost.

## What is already prepared

| Capability | Current implementation | Why it helps later |
|---|---|---|
| Business modules | `department`, `team`, `user`, `role`, and `security` packages | Makes dependencies and candidate boundaries visible |
| API contracts | Controllers expose DTO records, not JPA entities | Contracts can survive a persistence or service-boundary change |
| Database evolution | Flyway owns the schema; Hibernate validates it | Enables backward-compatible rolling deployments |
| Stateless requests | JWT resource server authentication | Instances do not rely on in-memory login sessions |
| Distributed cache | Redis through Spring Cache, with per-domain TTLs | Cache entries work across multiple API instances |
| Failure isolation | Cache errors fall back to PostgreSQL | Redis is an optimization, not an availability dependency |
| Request identity | `X-Correlation-Id` is accepted/generated, returned, and placed in logs | One request can be followed across service calls |
| Observability | Actuator, Micrometer, and Prometheus endpoint | Common operational contract for metrics and probes |
| Packaging | Multi-stage `Dockerfile` with a non-root runtime user | Produces a portable service image |
| External configuration | Database, Redis, JWT, TTL, and port settings use environment variables | The same image can run in several environments |

## Recommended future service boundaries

Start with two coarse services, not one service per entity:

```text
Client
  │
  ├── Identity service
  │     owns users, credentials, roles, permissions, token issuing
  │
  └── Organization service
        owns departments, teams, and membership projections
```

An API gateway can later provide one public hostname, routing, coarse rate limiting, and token relay. It must not become the place where business rules live.

The organization service should store an external `userId` for membership, not a foreign key into the identity service's database. Each service owns its tables and migrations. Cross-service reads happen through versioned APIs or local projections built from events—never by joining another service's database.

## Safe extraction plan

### Stage 0 — strengthen the modular monolith

1. Add ArchUnit or Spring Modulith verification so controllers cannot bypass services and modules cannot reach into another module's repositories.
2. Move shared business interactions behind explicit module-facing interfaces.
3. Add PostgreSQL and Redis Testcontainers tests.
4. Add audit events and idempotency keys for commands that clients may retry.
5. Measure real load before choosing a boundary.

### Stage 1 — introduce events without a broker dependency

Publish domain events after successful use cases, but persist externally important events in an **outbox table in the same PostgreSQL transaction**. A separate publisher sends them to Kafka or RabbitMQ and marks them delivered. Consumers keep an inbox/deduplication key so redelivery is safe.

Do not update PostgreSQL and publish directly to a broker and assume both succeeded. Without an outbox, a failure between those operations loses either the state change or the event.

### Stage 2 — extract one coarse service

1. Choose one boundary with independent ownership or scaling needs.
2. Define its HTTP/event contracts and compatibility policy first.
3. Create a separate Spring Boot application, database/schema, Redis namespace, Flyway history, and CI/CD pipeline.
4. Backfill its owned data and keep old/new paths synchronized during migration.
5. Switch callers behind an internal interface or strangler route.
6. Observe and verify, then delete the old path only after rollback is no longer needed.

### Stage 3 — production service-to-service behavior

- Set connection and response timeouts on every network call.
- Retry only safe/idempotent operations, with bounded exponential backoff and jitter.
- Add circuit breakers and bulkheads based on observed failure modes, not by default everywhere.
- Propagate `X-Correlation-Id` and W3C `traceparent` headers.
- Prefer workload identity or short-lived client credentials over shared static secrets.
- Use Kubernetes/service-platform DNS for discovery when deployed there; a separate discovery server is not automatically required.
- Add consumer-driven contract tests and backward-compatible rolling database changes.

## Redis design in this project

The application uses the cache-aside pattern through Spring Cache:

```text
request → cache lookup
              ├── hit  → return cached DTO
              └── miss → query PostgreSQL → cache DTO → return
```

Current caches and default TTLs:

| Cache | Key | Default TTL | Invalidation |
|---|---|---:|---|
| `departments` | department UUID | 10 minutes | put on create/update, evict on delete |
| `teams` | team UUID | 5 minutes | put on create/update, evict on delete |
| `users` | user UUID | 2 minutes | put on create/update/role changes, evict on password change/delete |
| `roles` | `catalogue` | 30 minutes | bootstrap catalogue is stable for the running version |

Updates that can change a nested representation also clear the affected dependent cache. Values are JSON, keys use the `galileo::<cache>::<key>` prefix, and nulls are never cached. If Redis is unavailable, a `CacheErrorHandler` logs the failure and the request continues against PostgreSQL. This favors availability but increases database load, so alert on Redis health and cache error logs.

In a real multi-service deployment, give each service its own key prefix and ownership. Do not let two services deserialize or mutate each other's cache entries. Redis persistence in local Compose is convenient for development; cache correctness must never depend on persistence.

Run only Redis while using the configured Neon database:

```powershell
docker compose up -d redis
.\mvnw.cmd spring-boot:run
```

Inspect cache keys during study:

```powershell
docker compose exec redis redis-cli --scan --pattern "galileo::*"
docker compose exec redis redis-cli TTL "galileo::roles::catalogue"
```

Production should point `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` at a managed Redis deployment, use TLS/network policy where supported, and store credentials in a secret manager.

## Deployment and observability checklist

- Build the image: `docker build -t galileo-api:local .`
- Keep one immutable image across environments; change only configuration.
- Use `/actuator/health/liveness` to decide whether to restart a process.
- Use `/actuator/health/readiness` to decide whether to route traffic to it.
- Scrape `/actuator/prometheus`; protect operational endpoints at the network or security layer.
- Set CPU/memory requests and limits, graceful termination time, and a disruption budget.
- Ship structured logs centrally and never log passwords, tokens, or connection strings.
- Add OpenTelemetry tracing before the first service extraction so cross-service latency is visible.
- Keep JWT signing material and Neon/Redis credentials outside Git and rotate leaked credentials.

## Patterns to study before splitting

Prioritize bounded contexts, database-per-service, transactional outbox/inbox, idempotency, eventual consistency, sagas, contract testing, observability, zero-downtime migrations, and failure injection. Those topics determine whether a distributed system behaves well; the number of Spring Cloud dependencies does not.
