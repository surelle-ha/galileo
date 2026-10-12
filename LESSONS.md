# GalileoAPI lessons and two-day Spring Boot study plan

## Where your starting code was

Your original API already demonstrated several important fundamentals:

- `@SpringBootApplication` and application startup
- `@RestController`, route mappings, request bodies, path variables, and query parameters
- constructor injection from controller → service → repository
- Java records for request/response data
- Bean Validation with `@Valid`, `@NotBlank`, and `@Email`
- centralized exceptions using `@RestControllerAdvice`
- basic CRUD and an email uniqueness rule

The main limitations were exactly the normal next learning boundary: the repository was an in-memory `Map`, domain records were also API payloads, a user had one hard-coded role enum, and there were no transactions, database constraints, migrations, authentication, pagination, or automated behavior tests.

GalileoAPI keeps the mental model you started with and replaces each simplified piece with the usual Spring enterprise equivalent.

## Read the code in this order

1. `GalileoApiApplication` — component scanning and typed configuration scanning.
2. `DepartmentController` — HTTP-only responsibilities.
3. `DepartmentService` — transaction boundary and business rules.
4. `DepartmentRepository` — Spring Data creates the implementation for this interface.
5. `DepartmentEntity` and `BaseEntity` — object/relational mapping and lifecycle callbacks.
6. `V1__create_organization_schema.sql` — the schema Hibernate validates against.
7. Repeat the path for `Team` and `User`, noticing their relationships.
8. `DataBootstrap` — roles are groups of permissions and the initial admin gets a role.
9. `SecurityConfig`, `AuthenticationService`, and `JwtService` — login and request authentication.
10. `CacheConfig` and the service cache annotations — Redis serialization, TTLs, and invalidation.
11. `CorrelationIdFilter` — request correlation for logs and downstream calls.
12. `OrganizationFlowIntegrationTests` — executable proof of the important flows.

Use the debugger. Put breakpoints in one controller, its service, and Hibernate's generated SQL logging; call the endpoint once and follow the entire path.

## Day 1: Spring, HTTP, persistence, and transactions

### Morning: the container and web layer

Study these ideas:

- **Dependency injection:** Spring creates beans and supplies dependencies. Constructor injection makes required dependencies visible and testable.
- **Component scanning:** stereotypes such as `@RestController`, `@Service`, and `@Configuration` register different kinds of beans.
- **Auto-configuration:** starters place libraries on the classpath; Boot conditionally configures common infrastructure.
- **HTTP semantics:** `POST` creates, `GET` reads, `PUT` replaces a resource representation, `PATCH` makes a partial change, and `DELETE` removes.
- **DTO validation:** validation rejects malformed requests at the boundary before business logic runs.
- **Status codes:** this API uses 201 for create, 204 for successful no-content operations, 400 for malformed input, 401 for invalid authentication, 403 for insufficient authority, 404 for a missing resource, and 409 for state conflicts.
- **Problem Details:** error responses use Spring's `ProblemDetail`, a standard shape clients can handle consistently.

Exercise: add a maximum team size setting and return a 409 when an assignment exceeds it. Keep parsing in the controller/DTO and the rule in the service.

### Afternoon: JPA and PostgreSQL

Understand the vocabulary:

- **JPA/Jakarta Persistence** is a specification: annotations and APIs for ORM.
- **Hibernate** is the implementation Spring Boot uses by default.
- **Spring Data JPA** builds repository implementations and query plumbing on top.
- **Entity** means persistent mutable state with identity. It is not an API DTO.
- **Persistence context** tracks managed entities during a transaction. Changes to a managed entity are flushed without an explicit repository `save` on every update.
- **Owning side** is the relationship side that writes the foreign key or join table.
- **LAZY versus EAGER** controls when related data is loaded. Eager loading is not a universal fix; it can produce oversized queries or N+1 query behavior.

Trace the model:

```text
teams.department_id  → departments.id
app_users.team_id    → teams.id
user_roles.user_id   → app_users.id
user_roles.role_id   → roles.id
role_permissions     → role + permission values
```

Why both Flyway and Hibernate?

- Flyway owns schema history and changes it deterministically.
- `ddl-auto=validate` makes Hibernate fail startup if entity mappings disagree with that schema.
- `ddl-auto=update` feels convenient but produces unreviewed, environment-dependent schema changes; do not make it the production migration strategy.

Why `@Transactional` belongs mainly on services:

- one use case can touch multiple repositories atomically;
- lazy relationships can be mapped while the persistence context is alive;
- rollback behavior is tied to a business operation, not an HTTP implementation detail;
- read-only transactions communicate intent and may enable optimizations.

Exercise: create a `V2__add_department_cost_center.sql` migration, update the entity and DTOs, then run the tests. Never edit V1 after it has reached a shared database.

## Day 2: security, testing, and operations

### Morning: authentication and RBAC

Keep these terms separate:

- **Authentication** proves who the caller is. GalileoAPI checks a BCrypt password at login and validates a signed JWT afterward.
- **Authorization** decides what that caller may do.
- **Role** is a business grouping such as `DEPARTMENT_MANAGER`.
- **Permission/authority** is a granular action such as `TEAM_WRITE`.

Request flow:

```text
email + password → BCrypt verification → signed JWT
Bearer JWT → signature/issuer/expiry validation → roles + permissions → @PreAuthorize
```

Passwords are one-way hashes. They are never decrypted and never returned. The application compares a submitted password with the stored BCrypt hash.

JWT tradeoffs:

- Verification is fast and stateless.
- A stolen token works until expiry unless you add revocation/session state.
- Permission changes do not affect an already-issued token until it expires.
- Symmetric HMAC means any service with the secret can sign tokens. Larger systems normally use asymmetric keys or delegate identity to Keycloak, Auth0, Okta, Entra ID, or another OpenID Connect provider.

RBAC is only one layer. Add resource ownership or attribute-based checks when a permission should be limited to a caller's own department/team.

Exercise: implement “department managers may update only users in their own department.” Put it in a reusable authorization service and reference it from `@PreAuthorize`.

### Afternoon: testing and operability

Testing layers to learn:

- **Unit test:** one class, mocked dependencies, very fast; best for branching business rules.
- **Web slice:** controller, serialization, validation, and security behavior without a full database.
- **Repository slice:** mappings and queries against a database test configuration.
- **Integration test:** the real Spring graph and multiple layers, as used here.
- **End-to-end test:** a deployed process called over the network with external dependencies.

The current tests prove context startup, migration compatibility, Hibernate schema validation, hierarchy persistence, bootstrap data, password matching, JWT creation, and JWT decoding. H2 is fast but not PostgreSQL; add Testcontainers next to catch PostgreSQL-specific SQL, types, locking, and query behavior.

Operational basics:

- Actuator exposes health, readiness/liveness probes, and metrics.
- HikariCP pools expensive database connections.
- externalized environment variables separate deploy-time secrets/settings from the artifact.
- structured logs, metrics, and distributed traces answer different questions; learn all three.
- graceful shutdown stops accepting work and lets in-flight work finish.

Caching basics:

- PostgreSQL remains the source of truth; Redis stores disposable copies of response DTOs.
- `@Cacheable` reads through the cache, `@CachePut` refreshes it after a successful method, and `@CacheEvict` removes stale values.
- a TTL limits how long a missed invalidation can remain visible, but TTL alone is not a consistency strategy.
- cache keys and value formats become compatibility contracts during rolling deployments.
- a cache outage should have an explicit policy. GalileoAPI fails open to PostgreSQL and logs a warning, trading higher database load for availability.
- Spring caching is proxy-based. Like `@Transactional` and `@PreAuthorize`, a cached method called through `this` does not pass through the proxy.

Exercise: add Testcontainers PostgreSQL and Redis, assert that a second role-catalogue read is served from Redis, then assert that the endpoint still returns 200 while Redis is stopped.

## Microservices lesson: start with boundaries, not processes

The current application is a modular monolith, which keeps one reliable ACID transaction while you learn. A microservice split removes cross-module Java calls and cross-table transactions and replaces them with networks, versioned contracts, retries, partial failures, and eventual consistency.

Before extracting anything, be able to explain:

- which service owns each datum and is the only writer;
- how another service obtains a copy without reading the owner's database;
- what happens when a downstream call times out after doing the work;
- how events are published atomically with database changes (transactional outbox);
- how consumers handle duplicate and out-of-order delivery;
- how one request is traced across services;
- how contracts and database schemas remain compatible during rolling deployment.

Study the concrete staged plan in [MICROSERVICES.md](MICROSERVICES.md). A good exercise is to enforce module boundaries with ArchUnit or Spring Modulith before creating a second deployable application.

## Important details hidden in plain sight

- `BaseEntity.version` enables optimistic locking so concurrent edits can be detected instead of silently overwriting each other.
- unique checks in services give friendly messages, but database unique constraints are still required because concurrent requests can pass the check simultaneously.
- `open-in-view=false` prevents controllers/serializers from silently querying the database after the service transaction; map DTOs inside the service boundary.
- entities are mutable because Hibernate tracks state; request/response DTOs are records because value semantics fit them.
- repository method names are parsed into queries. Use explicit JPQL when filters become more complex, and verify generated SQL.
- pagination prevents an unbounded `findAll()` from loading an entire company into memory.
- API versioning (`/api/v1`) gives room for incompatible future contracts, but it does not remove the need for deprecation discipline.
- `@PreAuthorize` is enforced by a proxy. Calls from one method to another method on the same object do not pass through that proxy—learn the general “self-invocation” limitation of Spring AOP.

## Questions you should be able to answer after studying

1. Why is `UserEntity` not returned directly from a controller?
2. What creates the implementation of `UserRepository`?
3. What is the difference between JPA, Hibernate, and Spring Data JPA?
4. Why are service update methods transactional even without an explicit `save`?
5. Why do service uniqueness checks not replace database unique constraints?
6. When would a lazy association fail to load, and why is Open Session in View disabled?
7. What does Flyway record, and why must an applied migration remain immutable?
8. How does a role become a set of `GrantedAuthority` values in an authenticated request?
9. Why does a valid JWT not guarantee that its permissions are still current?
10. Which tests require real PostgreSQL rather than H2?
11. Why is Redis not allowed to become the source of truth?
12. Why can two services not safely share tables and still claim independent ownership?

## After the two days

Build one feature yourself without copying: invitations, audit history, leave requests, or projects. Include a migration, entity, DTOs, repository, transactional service, permission, controller, OpenAPI description, and tests. That vertical slice will teach more than adding ten unrelated annotations.

See [ENTERPRISE_LIBRARIES.md](ENTERPRISE_LIBRARIES.md) for a prioritized learning backlog.
