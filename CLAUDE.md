# payment-webhook-service

Spring Boot 3.5 app on Java 21, built with Gradle (Groovy DSL, wrapper committed). PostgreSQL 16 is the only datastore; Flyway owns the schema.

## Build

- `./gradlew build` — full build, includes `check` (tests, Spotless, JaCoCo gate).
- `./gradlew spotlessCheck` / `spotlessApply` — formatting check / auto-fix.
- `docker compose up -d` — starts local Postgres for running the app outside tests.
- All dependency and plugin versions live only in `gradle.properties`; never hardcode a version literal in `build.gradle`.

## Package layout (hexagonal)

Packages under `io.github.darklight606.paymentwebhook`:

- `domain.model`, `domain.exception`, `domain.port.in`, `domain.port.out` — no Spring/JPA/Hibernate imports, no dependency on `application` or `adapter`.
- `application` — no dependency on `adapter`.
- `adapter.in.rest`, `adapter.in.scheduler`, `adapter.out.persistence`, `adapter.out.ledger`, `adapter.out.acmepay` — implements ports, wires frameworks.
- `config` — Spring configuration/wiring.

These rules are enforced by ArchUnit in `src/test/java/.../ArchitectureTest.java`, which also proves each rule actually fires using fixture violations in `archfixture/`. Every package must keep its `package-info.java`.

## Testing

- Integration tests are named `*IT` and use `TestcontainersConfiguration` (`postgres:16-alpine` via `@ServiceConnection`) — no H2, no mocked datastore.
- JaCoCo enforces an 80% line-coverage gate on `check`, excluding only `PaymentWebhookApplication.class`.

## Conventions

- No employer name, internal service/library name, internal URL, or real payment vendor name in any file. The only provider is the fictional AcmePay; the only downstream is a fake ledger API.
- Commit messages: Conventional Commits, `<type>: <description>`, imperative, lower case, no ticket id.
- Prefer the plainest construct that does the job: no speculative abstractions, no placeholder classes, no unused extension points.
