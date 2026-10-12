# Nice-to-have Spring libraries and enterprise modules

Do not add everything at once. Every dependency expands the upgrade, security, configuration, and operational surface. Add a tool when a concrete requirement justifies it and test its failure modes.

## Learn next: highest value

| Library/module | Learn it for | Suggested GalileoAPI exercise |
|---|---|---|
| Testcontainers (`testcontainers-postgresql`) | Integration tests against disposable real infrastructure | Replace or supplement H2 with PostgreSQL tests |
| Testcontainers (`testcontainers-redis`) | Real cache integration and outage testing | Verify cache hit, TTL, invalidation, and fail-open behavior |
| Spring Security OAuth2 Client/Resource Server | Standards-based OpenID Connect and external identity providers | Replace local login with Keycloak |
| Spring Boot Actuator + Micrometer | Health, application metrics, and operational endpoints | Add request latency and user-creation counters |
| Micrometer Tracing + OpenTelemetry | Correlation and distributed traces across services | Export traces to Grafana Tempo or Jaeger |
| Resilience4j | Timeouts, retries, circuit breakers, bulkheads, rate limits | Protect a simulated downstream HR service |
| MapStruct | Compile-time DTO/entity mapping with explicit mappings | Replace repetitive `from(...)` mapping methods |
| ArchUnit | Automated architecture boundaries | Prevent controllers from calling repositories directly |
| Awaitility | Reliable assertions for asynchronous behavior | Test an event-driven audit writer |

## Data and persistence

| Library/module | When it helps | Caveat |
|---|---|---|
| Spring Data JDBC | Aggregates that need simpler persistence than a full ORM | Different model from JPA; do not casually mix mappings |
| jOOQ | SQL-heavy systems needing type-safe, explicit queries | Code generation and SQL knowledge are central |
| Querydsl or JPA Specifications | Dynamic filtering and composable predicates | Watch query complexity and count-query performance |
| Hypersistence Utils | Advanced Hibernate/PostgreSQL types and diagnostics | Verify compatibility with your Hibernate major version |
| Hibernate Envers | Entity revision history | Not a substitute for a business audit/event model |
| Liquibase | Alternative to Flyway, especially rich change-set workflows | Choose one migration owner, not both |
| Spring Data Redis | Caching, distributed coordination, ephemeral data | **Included now** for shared JSON caches; next study invalidation races and Redis Cluster |
| Spring Cache abstraction + Caffeine | Simple local caching | Local caches differ on every application instance |

## Messaging and asynchronous work

| Library/module | Use case | What to study |
|---|---|---|
| Spring for Apache Kafka | Durable event streams and high-throughput integration | partitions, consumer groups, ordering, retries, schema evolution |
| Spring AMQP/RabbitMQ | Queued commands and routing patterns | acknowledgements, dead-letter exchanges, redelivery |
| Spring Modulith | Strong module boundaries and domain events in a modular monolith | module verification, event publication registry |
| Quartz or Spring Batch | Durable schedules and large restartable jobs | job state, retries, idempotency, chunking |

For database plus messaging, study the transactional outbox pattern. A database transaction cannot magically make a separate message broker atomic.

## API and integration

| Library/module | Use case | Note |
|---|---|---|
| Spring HTTP Service Clients / `RestClient` | Calling synchronous HTTP services | Set connection/read timeouts and instrument calls |
| Spring WebFlux `WebClient` | Non-blocking HTTP and reactive systems | Do not mix reactive and blocking code without understanding thread use |
| Spring Cloud OpenFeign | Declarative service clients | Useful, but avoid hiding timeout/retry behavior |
| Spring for GraphQL | Client-driven graph queries | Authorization and N+1 data fetching still matter |
| Protocol Buffers + gRPC | Strong contracts and efficient service-to-service RPC | Requires schema compatibility discipline |
| springdoc-openapi | API discovery and client generation | Already included; learn examples, schemas, and grouped APIs |

## Security

| Library/module | Use case |
|---|---|
| Keycloak | Self-hosted OAuth 2.0/OpenID Connect identity and role administration |
| Spring Authorization Server | Building an authorization server when that truly is your product requirement |
| OWASP Dependency-Check or Snyk | Dependency vulnerability visibility in CI |
| Bucket4j | Local or distributed API rate limiting |
| HashiCorp Vault / cloud secret managers | Runtime secret retrieval and rotation |

Prefer a proven identity provider over growing the sample username/password implementation into a home-built identity platform. For browser clients, study authorization code flow with PKCE; for service clients, study client credentials and workload identity.

## Quality and developer workflow

| Tool | Value |
|---|---|
| Spotless | Deterministic formatting in local development and CI |
| Checkstyle | Team-specific source conventions |
| Error Prone / NullAway | Compile-time bug and nullness detection |
| SonarQube/SonarCloud | Broader static analysis and quality trends |
| PIT | Mutation testing to discover weak assertions |
| JaCoCo | Coverage feedback (a signal, not a quality target) |
| WireMock | Controllable HTTP dependency tests |
| REST Assured | Readable black-box API tests |
| Spring REST Docs | Documentation generated from verified tests |

## Delivery and platform

- **Paketo Buildpacks or Jib:** reproducible container images without hand-maintaining every Docker layer.
- **Dockerfile + Docker Compose:** included now for the API image and local Redis/PostgreSQL dependencies.
- **Docker Compose support:** convenient local dependency startup.
- **Kubernetes:** deployments, services, configuration, probes, resources, autoscaling, and disruption—not just YAML syntax.
- **Helm or Kustomize:** environment-aware Kubernetes packaging.
- **OpenTelemetry Collector:** vendor-neutral telemetry routing.
- **Prometheus + Grafana + Loki/Tempo:** metrics, dashboards, logs, and traces.
- **GitHub Actions, GitLab CI, Jenkins, or Azure Pipelines:** repeatable test, scan, package, and deployment stages.
- **Spring Cloud Config:** centralized configuration when the operational need appears; secret managers should still own secrets.

## Architectural topics more important than another library

Study these alongside the tools:

- modular monoliths before automatic microservices;
- domain boundaries and aggregate consistency;
- idempotency for retried commands;
- optimistic versus pessimistic locking;
- transaction isolation and race conditions;
- database indexing and query plans;
- API compatibility, deprecation, and consumer-driven contracts;
- outbox/inbox and saga patterns;
- multi-tenancy and resource-scoped authorization;
- threat modeling, least privilege, and secure defaults;
- service-level indicators/objectives, alert quality, and incident response;
- zero-downtime database migrations and backward-compatible deployments.

## A sensible learning sequence

1. Testcontainers PostgreSQL and more HTTP/security tests.
2. External OpenID Connect provider (Keycloak is convenient for learning).
3. Prometheus metrics are included; add dashboards, structured JSON logging, and OpenTelemetry traces.
4. MapStruct and ArchUnit as the codebase grows.
5. One outbound HTTP integration with explicit timeout and Resilience4j policy.
6. One asynchronous workflow with Kafka or RabbitMQ plus an outbox.
7. Containerization and CI/CD.
8. Only then evaluate whether service boundaries justify microservices.
