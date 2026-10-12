# GalileoAPI

GalileoAPI is a study-sized, enterprise-style Spring Boot backend for managing this hierarchy:

```text
Department 1 ─── * Team 1 ─── * User * ─── * Role 1 ─── * Permission
```

It deliberately goes beyond a tutorial CRUD application: PostgreSQL, JPA/Hibernate, Flyway migrations, Redis caching, transactions, DTOs, pagination, validation, RFC 9457-style problem responses, JWT authentication, permission-based RBAC, Swagger UI, observability, and integration tests are wired together as one coherent example.

## Technology choices

| Concern | Choice | Why |
|---|---|---|
| Platform | Java 21 target, Spring Boot 4.1.1 | Current Spring generation while targeting a commonly deployed LTS Java level |
| ORM | Spring Data JPA + Hibernate | JPA is the standard API; Hibernate is Boot's default implementation |
| Database | PostgreSQL | Strong relational model and production-ready constraints/indexes |
| Schema | Flyway | Versioned, reviewable database changes instead of Hibernate mutating production schemas |
| Cache | Spring Cache + Redis (Jedis client) | Shared cache across instances, explicit TTLs, and PostgreSQL fallback |
| Security | Spring Security + signed JWT | Stateless API authentication with permissions in token claims |
| API docs | springdoc-openapi + Swagger UI | Generated OpenAPI docs with an interactive client |
| Boilerplate | Lombok | Constructor/getter/log generation; business logic remains ordinary Java |
| Operations | Actuator + Micrometer/Prometheus + HikariCP | Health/metrics endpoints and production-grade connection pooling |
| Tests | JUnit, AssertJ, Spring Boot Test, H2 | Fast isolated verification of migrations, ORM, hierarchy, and JWT flow |

Official references: [Spring Boot SQL/JPA](https://docs.spring.io/spring-boot/reference/data/sql.html), [Spring Security](https://docs.spring.io/spring-security/reference/), [Flyway](https://documentation.red-gate.com/flyway), and [springdoc-openapi](https://springdoc.org/).

## Architecture

The code is organized by business feature instead of by one global `controller/service/repository` folder:

```text
src/main/java/com/galileo/api/
├── bootstrap/       role catalogue and first administrator
├── common/          API envelope, pagination, base entity, exception handling
├── config/          typed settings, Spring Security, OpenAPI, Redis cache
├── department/      department controller/service/repository/entity/DTOs
├── team/            team controller/service/repository/entity/DTOs
├── user/            user controller/service/repository/entity/DTOs
├── role/            role and permission model
└── security/        login, JWT issuing, JWT-to-authority conversion
```

The request path is:

```text
HTTP request
  → Spring Security (verify JWT)
  → Controller (HTTP mapping + validation)
  → Service (transaction + business rules + cache-aside)
  → Redis cache hit, or repository on a miss
  → Spring Data repository
  → Hibernate (JPA implementation)
  → PostgreSQL
```

Controllers never return JPA entities. DTOs prevent password hashes, lazy proxies, and accidental relationship recursion from becoming part of the public contract.

## Data and authorization model

- A team belongs to exactly one department.
- A user can belong to zero or one team. A user without a team is useful for administrators or new hires.
- A user has one or more roles.
- A role groups permissions. Controllers check permissions such as `TEAM_WRITE`, not hard-coded role names.
- Department deletion is rejected while it owns teams; team deletion is rejected while it owns users.
- Foreign keys, uniqueness constraints, indexes, and optimistic-lock `version` columns reinforce application rules at the database layer.

The bootstrap role catalogue is:

| Role | Intended access |
|---|---|
| `ADMIN` | Every permission, including role assignment |
| `DEPARTMENT_MANAGER` | Manage departments, teams, and user profiles; cannot assign roles |
| `TEAM_LEAD` | Read departments, teams, and users |
| `USER` | Read the organization directory and change their own password |

RBAC answers “may this role perform this operation?” A real multi-tenant system often adds resource-level rules too, such as “a department manager may only edit users in their department.” That is a good next exercise; it is intentionally called out rather than hidden behind a misleading claim of production completeness.

The feature-oriented modular monolith is the deliberate starting point for later extraction. See [MICROSERVICES.md](MICROSERVICES.md) for service boundaries, data ownership, outbox/event guidance, and a staged migration plan.

## Run with the supplied Neon database

Prerequisites: Java 21+, Docker Desktop (for Redis), and PowerShell. The Maven wrapper is included. This workspace already has the supplied Neon connection in an ignored `.env` file.

1. Start Redis:

   ```powershell
   docker compose up -d redis
   ```

2. Optionally replace the development secrets in the current terminal:

   ```powershell
   $env:JWT_SECRET = "replace-with-a-long-random-development-secret"
   $env:BOOTSTRAP_ADMIN_EMAIL = "admin@galileo.local"
   $env:BOOTSTRAP_ADMIN_PASSWORD = "a-new-strong-password"
   ```

3. Start the API:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

Flyway creates/validates the schema on Neon and the bootstrap component creates the RBAC catalogue plus the first administrator. With untouched local defaults, login is `admin@galileo.local` / `Admin123!`. These defaults are only for local study—change them before shared use.

4. Open [Swagger UI](http://localhost:8080/swagger-ui.html) or check [health](http://localhost:8080/actuator/health).

## Database and secret handling

Spring imports the local `.env` automatically, and `.gitignore` prevents it from being committed. The credential is not present in `application.yml`, `.env.example`, or any tracked file. If this project is cloned elsewhere, create `.env` from `.env.example` or supply the settings as environment variables.

Because the current password was shared in chat, rotate it in Neon and update the ignored `.env` afterward. The optional `postgres` Compose service remains available for offline study, but it is not used while `DB_URL` points at Neon.

## Redis caching

Spring Cache stores department, team, user, and role response DTOs in Redis. Cache writes and invalidations are colocated with transactional service methods, values have per-domain TTLs, and related caches are cleared when a parent change can make nested data stale. Redis is not the system of record: if it is unavailable, cache operations fail open and requests continue against Neon PostgreSQL.

Useful development commands:

```powershell
docker compose ps redis
docker compose exec redis redis-cli --scan --pattern "galileo::*"
docker compose exec redis redis-cli TTL "galileo::roles::catalogue"
```

Read [MICROSERVICES.md](MICROSERVICES.md#redis-design-in-this-project) for the cache key/TTL table and production guidance.

## Authenticate and call the API

PowerShell example using the default local administrator:

```powershell
$loginBody = @{
  email = "admin@galileo.local"
  password = "Admin123!"
} | ConvertTo-Json

$login = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/auth/login" `
  -ContentType "application/json" `
  -Body $loginBody

$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }

$departmentBody = @{
  code = "ENG"
  name = "Engineering"
  description = "Builds the Galileo platform"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/departments" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $departmentBody
```

In Swagger UI, call `POST /api/v1/auth/login`, copy `data.accessToken`, click **Authorize**, and enter the token. Swagger adds the `Bearer` prefix.

## Endpoint overview

| Method and path | Purpose | Required access |
|---|---|---|
| `POST /api/v1/auth/login` | Issue access token | Public |
| `GET /api/v1/auth/me` | Current user | Authenticated |
| `GET/POST /api/v1/departments` | Search/create departments | `DEPARTMENT_READ/WRITE` |
| `GET/PUT/DELETE /api/v1/departments/{id}` | Read/update/delete department | `DEPARTMENT_READ/WRITE` |
| `GET/POST /api/v1/teams` | Filter/create teams | `TEAM_READ/WRITE` |
| `GET/PUT/DELETE /api/v1/teams/{id}` | Read/update/delete team | `TEAM_READ/WRITE` |
| `GET/POST /api/v1/users` | Filter/create users | `USER_READ`; creation also needs `ROLE_ASSIGN` |
| `GET/PUT/DELETE /api/v1/users/{id}` | Read/update/delete user | `USER_READ/WRITE` |
| `PUT /api/v1/users/{id}/roles` | Replace role assignments | `ROLE_ASSIGN` |
| `PATCH /api/v1/users/{id}/password` | Reset password | Self or `USER_WRITE` |
| `GET /api/v1/roles` | View role catalogue | `ROLE_READ` |

List endpoints accept Spring pagination parameters such as `?page=0&size=20&sort=name,asc`. They also accept the documented `query`, `teamId`, or `departmentId` filters.

## Configuration

| Environment variable | Default | Meaning |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/galileo` | JDBC connection URL |
| `DB_USERNAME` / `DB_PASSWORD` | `galileo` / `galileo` | Database credentials |
| `DB_POOL_SIZE` | `10` | Maximum Hikari connections |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis endpoint |
| `REDIS_PASSWORD` | empty | Redis authentication; set outside Git |
| `REDIS_CONNECT_TIMEOUT` / `REDIS_COMMAND_TIMEOUT` | `2s` / `1s` | Fail-fast cache timeouts |
| `DEPARTMENT_CACHE_TTL` | `PT10M` | Department cache lifetime |
| `TEAM_CACHE_TTL` | `PT5M` | Team cache lifetime |
| `USER_CACHE_TTL` | `PT2M` | User cache lifetime |
| `ROLE_CACHE_TTL` | `PT30M` | Role catalogue cache lifetime |
| `JWT_SECRET` | Local-only development value | HMAC signing key; use 32+ random characters |
| `JWT_ISSUER` | `galileo-api` | Expected JWT issuer |
| `JWT_TTL` | `PT1H` | Token lifetime as ISO-8601 duration |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` | Create roles and first admin on startup |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@galileo.local` | First admin email |
| `BOOTSTRAP_ADMIN_PASSWORD` | `Admin123!` | First admin password; override it |

## Build and test

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
```

Tests use an in-memory H2 database in PostgreSQL mode. They run the same Flyway migration and ask Hibernate to validate it. For production confidence, the next testing step is Testcontainers with a real PostgreSQL image (see `ENTERPRISE_LIBRARIES.md`).

## Production gaps to study

This is an enterprise-shaped learning application, not a claim of production readiness. Before deployment, add asymmetric keys or an external identity provider, refresh-token/session revocation strategy, resource-scoped authorization, secret management, audit events, rate limiting, container/image scanning, PostgreSQL-backed integration tests, centralized logs/traces, CI/CD, backups, and explicit CORS/network policy.

Continue with [LESSONS.md](LESSONS.md) for a two-day study path, [MICROSERVICES.md](MICROSERVICES.md) for the extraction roadmap, and [ENTERPRISE_LIBRARIES.md](ENTERPRISE_LIBRARIES.md) for the next modules worth learning.
