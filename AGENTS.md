# CRM repository guidance

## Scope and system overview

This repository is a Maven reactor containing two Spring Boot applications and two framework-neutral Java libraries:

- `crm-gateway` — the external entry point. It validates Avanpost FAM JWTs, resolves an external login/email, matches the request to a database-backed route rule, asks `crm-user-service` for an RBAC decision, adds trusted internal identity headers, and proxies the request.
- `crm-user-service` — the source of truth for users, roles, permissions, groups, and authorization decisions.
- `crm-user-service-client` — a Spring-independent OkHttp client and DTO contract for the user-service authorization API.
- `crm-internal-identity` — a Spring/Servlet-independent contract for reading and writing trusted internal user identity headers.

Keep a change in the owning module. Put a cross-module HTTP or header contract in the corresponding library rather than duplicating it in applications. When a shared contract changes, update all producers, consumers, documentation, and tests in the same change.

## Architectural boundaries and patterns

The repository uses a multi-module service architecture with an API Gateway and layered application modules.

- Preserve the dependency direction: `crm-gateway` may depend on both libraries; the libraries must not depend on either Spring Boot application; `crm-user-service` must not depend on the gateway.
- In an application module, keep transport concerns in `api` or `routing`, use-case orchestration in `application`, security/provider integration in `security`, outbound client wiring in `user`, and persistent business state plus repositories in `domain`.
- Controllers and servlet filters translate protocols; they must not contain persistence queries or duplicate authorization rules.
- Application services own use-case decisions and transaction boundaries. Keep reads such as authorization checks `@Transactional(readOnly = true)`.
- Spring Data repositories are the persistence abstraction used by the current code. Put query semantics there, not in controllers, filters, or routing configuration.
- Use immutable records/value objects for request-independent data where practical. JPA entities may use focused Lombok annotations but must retain protected no-arg constructors and controlled mutation.

Important patterns currently in use are Gateway/Proxy, layered architecture, Repository, application service, DTO/client adapter, configuration properties, and deny-by-default authorization. Do not introduce a competing abstraction unless the change genuinely requires it.

## Module layout

### `crm-gateway`

- `api` — authorization filter and the request wrapper that supplies trusted headers.
- `application` — ordered route-rule compilation and matching.
- `domain` — `RouteRule` JPA entity and repository.
- `routing` — Spring Cloud Gateway MVC route construction and proxy rewriting.
- `security` — stateless resource-server configuration and Avanpost identity resolution.
- `user` — `crm-user-service-client` configuration and typed properties.
- `src/main/resources/db/changelog` — route-rule schema and environment-scoped seed data.

Route rules are loaded from the database at application startup, ordered by `rule_order`, and the first match wins. A rule couples HTTP method/path, required permission, and target URI. Keep authorization matching and proxy routing based on the same rule model. If runtime rule editing or hot reload is added, refresh both views consistently.

### `crm-user-service`

- `api` — internal HTTP endpoint, request/response DTOs, and exception translation.
- `application` — authorization use case and denial reasons.
- `domain/model` — JPA user, role, permission, and administrative group models.
- `domain/repository` — Spring Data repositories and RBAC queries.
- `src/main/resources/db/changelog` — user/RBAC schema and seed data.

### Shared libraries

- `crm-user-service-client` owns the wire contract, error taxonomy, serialization, and OkHttp adapter for calls to the user service. The caller owns and reuses the `OkHttpClient` lifecycle.
- `crm-internal-identity` owns trusted header names and serialization. Keep it independent of Spring and both Servlet APIs so downstream services can use it from any HTTP stack.

The two libraries target Java 11. The Spring Boot applications target Java 21. Do not use Java 12+ APIs or language features in shared libraries.

## Security and authorization invariants

- Authentication at the gateway is stateless OAuth2 Resource Server JWT authentication. Do not add HTTP sessions for authenticated traffic.
- Authorization is RBAC only: `User -> Role -> Permission`. User groups are hierarchical administrative metadata and do not grant permissions.
- Default to deny. Unknown routes, missing identities, unknown or inactive users, conflicting login/email matches, and absent permissions must never be authorized.
- `User.id` is the stable UUID identity propagated downstream. Login and email are mutable matching attributes, not downstream identities.
- If login and email are both supplied, both must resolve and identify the same user. Normalize login/email consistently and permission codes consistently before querying.
- A denied user-service decision is a normal HTTP 200 response with `allowed: false`; malformed contracts are HTTP 400. Gateway authentication failures are 401, authorization/identity failures are 403, and unavailable identity/user services are 503.
- The gateway alone creates trusted internal identity headers from the allowed decision's `userId` and `userVersion`. Strip or overwrite spoofed inbound values; downstream services must use `crm-internal-identity` to consume them.
- Email is usable only according to `email_verified` and `IDENTITY_TRUST_UNVERIFIED_EMAIL`. Preserve the configured JWT, USERINFO, and fallback modes.
- Keep internal endpoints suitable for a trusted Gateway-only network boundary. Do not expose user-service administration or authorization endpoints externally without an explicit authentication and authorization design.
- Actuator health is the only currently anonymous gateway endpoint. Treat any expansion of anonymous endpoints as a security-sensitive change.

## Database migrations

Both application modules use Liquibase and support file-based H2 locally plus PostgreSQL at runtime.

- Use Liquibase XML changelogs only. Do not add YAML or formatted SQL changelogs.
- Name migration files `<major>.<minor>.<patch>-<sequence>.xml`, for example `1.0.0-003.xml`, and include them from that module's `db.changelog-master.xml`.
- Never alter a changeset that may already have been applied. Add a new sequential changeset instead.
- Prefer declarative Liquibase change types. Use raw SQL only when no standard change type expresses the operation, and explain why in the changeset comment.
- Keep demo/local data in an environment-scoped changeset (`dev` in the gateway, currently `local` in the user service). Store bulk data under `db/changelog/data/<migration-name>/` and load it with `loadData`. Production migrations must not depend on demo data.
- Add explicit rollback for data changes and for schema operations Liquibase cannot safely reverse.
- Preserve database constraints and indexes that enforce identity uniqueness, relationship integrity, and ordered route lookup.

## Configuration and local development

- Supply defaults in each module's `application.yml` and deployment overrides through environment variables. Never commit credentials, tokens, or machine-specific absolute paths.
- Keep `spring.jpa.open-in-view=false` and `hibernate.ddl-auto=validate`; Liquibase owns the schema.
- Keep `AUTO_SERVER=TRUE` in the default file-based H2 URLs so external database tools can connect while an application runs.
- Do not commit generated `target/`, `.m2-local/`, `*.mv.db`, or `*.trace.db` files. Liquibase CSV files are source assets and must be committed.
- Use `@ConfigurationProperties` for grouped application settings rather than scattering direct environment/property lookups through business code.

## Lombok

- Use Lombok where annotation processing is supported. Declare it as an optional/provided compile-time dependency and configure it as a Maven annotation processor; it must not become a runtime dependency.
- Prefer focused annotations such as `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@EqualsAndHashCode`, and `@NoArgsConstructor`.
- Avoid `@Data` on JPA entities and security/domain types because it can expose unsafe setters, equality, or string representations.
- Keep explicit constructors and methods when they enforce validation, normalization, invariants, or other domain behavior. Lombok must not hide business logic.

## Verification

Use the Maven wrapper if one is added later; currently use the installed Maven executable.

From the repository root, run the complete reactor tests:

```powershell
mvn test
```

For a focused module change, include its upstream reactor dependencies:

```powershell
mvn -pl crm-user-service -am test
mvn -pl crm-gateway -am test
```

Add focused tests for every changed authorization outcome, identity-resolution rule, route match/order behavior, trusted-header behavior, client wire contract, or API validation rule. When changing migrations, test with a clean database so Liquibase applies every changeset. Do not rely only on an existing local H2 file.
