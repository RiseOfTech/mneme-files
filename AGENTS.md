# mneme-files — Project guidelines

`mneme-files` is the file store of the **Mneme** system: a domain-agnostic "storekeeper"
in front of S3-compatible object storage. It stores bytes and returns them by id. It
knows nothing about cards, users, or any other domain concept.

Product scope lives in `PROJECT.md`; the authoritative design lives in `ARCHITECTURE.md`.
Read `ARCHITECTURE.md` before implementing — if it is absent, the architecture has not
been decided yet, so do not invent structure: ask.

## Stack

- Language: **Java 21 — all production and test source is Java**
- Framework: Spring Boot 4.1 (Spring Framework 7, Jakarta EE 11, Jackson 3)
- Build: Gradle with the **Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`) —
  build scripts only; this does not make the project a Kotlin project
- Storage: S3-compatible object storage, accessed through AWS SDK v2 (`s3`).
  SeaweedFS in dev and test; the service is not coupled to any particular
  implementation.
- Persistence: **none — this service has no relational database** (see rules)
- Group: `io.github.ifamneme`
- Root package: `io.github.ifamneme.mneme_files` (sub-package layout per
  `ARCHITECTURE.md`)

## Build and run

- Build: `./gradlew build`
- Test: `./gradlew test`
- Run: `./gradlew bootRun`
- Health (k8s probes): `GET /actuator/health`

Run `./gradlew build` after any significant change to confirm it compiles and tests pass.

## Project-specific rules

### Java source, Kotlin build scripts

The build scripts are Kotlin; the project is not. Keep the two apart:

- **Do NOT write Kotlin source files.** No `.kt` files, no `src/main/kotlin`, no Kotlin
  plugin, no `kotlin-stdlib` dependency. Every class, record, and test is Java. Use Java
  records where a value type is needed — never a Kotlin `data class`.
- **Do NOT write Groovy syntax in `.kts` files.** Dependencies are
  `implementation("group:artifact:version")` with parentheses and double quotes; plugins
  use the `plugins { id("...") version "..." }` block; the Java version is set through a
  toolchain, not `sourceCompatibility`.
- If a build snippet found in documentation or search results is Groovy, translate it to
  the Kotlin DSL rather than pasting it.

### This service is deliberately dumb

- **Do NOT introduce any domain concept.** No cards, no users, no albums, no owners, no
  "image for X" endpoints, no entity names in types, packages, or paths. The API is about
  files and ids, nothing else. If a change requires knowing what a file is *for*, the
  change belongs in another service.
- **Do NOT store file↔entity relationships.** That association lives in the service that
  owns the entity (`mneme-core`, `mneme-auth`), never here.
- **Do NOT implement domain authorization** ("is this your file"). This service cannot
  answer that question and must not try. Write access requires only a valid signature.

### No database

- **Do NOT add persistence dependencies** — no Spring Data JPA/JDBC, no Postgres driver,
  no Flyway, no JPA entities, no `Testcontainers` Postgres module. This is a decision,
  not an omission: object metadata (content type, size, ETag, last modified) is stored on
  the object itself and read back from the storage, so there is nothing left for a
  database to hold.
- Anything that looks like it needs a table (quotas, per-user aggregates, listings,
  soft deletes, audit tables) is out of scope. Do not add it; raise it instead.

### Storage access

- All object storage access goes through the outbound port declared by the inner layer.
  No AWS SDK type may appear outside its adapter — not in the domain, not in the
  application layer, not in controllers. This is a checkable invariant.
- The port has exactly three operations: store, load, delete. **Do NOT add `exists`,
  `list`, `copy`, `move`, `head`, or batch variants** — nothing in this service needs
  them, and each one invites domain logic.
- **Do NOT create the bucket at application startup.** The bucket is provisioned by the
  environment (compose init step, test setup). The service assumes it exists and fails
  fast if it does not.
- Byte payloads cross the port as `java.io.InputStream`, never as `byte[]`. **The caller
  closes the stream** — an unclosed response stream leaks an HTTP connection from the
  SDK's pool. Document this on the port and honour it at every call site.
- The AWS SDK v2 client must be configured for **path-style access** against non-AWS
  endpoints, with static credentials from configuration. No AWS profile files, no
  credential provider chains, no instance metadata lookups.

### Files and ids

- File ids are **UUIDv4** (`UUID.randomUUID()`). **Do NOT use UUIDv7, ULID, or any
  time-ordered or sequential scheme.** Reads are unauthenticated, so an unguessable id is
  the only access control on a file; time-ordered ids leak a shared prefix and shrink the
  search space. This is a security requirement, not a style preference.
- Ids are generated in the domain, never by the adapter and never by the storage.
- The object key is the id's string form: flat, no prefixes, no directory sharding, no
  file extension. Extensions in keys create a second, competing source of truth about the
  content type.

### Uploads

- **Do NOT trust the client-declared `Content-Type`.** The stored and served content type
  is determined from the file's own leading bytes and matched against the allowlist. The
  declared value takes no part in the decision.
- Content-type detection is a fixed allowlist check against known magic-byte signatures.
  **Do NOT add Apache Tika or any other general content-detection library** — the task is
  to confirm one of a few permitted formats, not to identify arbitrary ones.
- Size and content-type limits are configuration-driven, expressed in the domain, and
  enforced there as well as by the multipart parser. Do not hard-code the numbers in
  business logic.

### API

- REST endpoints are versioned under `/v1`.
- Errors are `application/problem+json` with a `code` property, matching `mneme-core`'s
  conventions. Success responses and status codes are specified per slice — implement
  what the spec names and nothing else.
- **Do NOT configure CORS.** Uploads are server-to-server; downloads are plain `<img>`
  loads, which need no CORS. Adding it would be speculative configuration.
- **Do NOT implement presigned URLs, direct-to-storage uploads, range requests,
  conditional requests (304), or image processing** (resizing, thumbnails, format
  conversion). All are named, deliberate non-goals — see `PROJECT.md`.

### Security

- This service is an OAuth2 resource server. It verifies token signatures with
  `mneme-auth`'s **public** key, supplied as a mounted PEM (SPKI) via configuration.
  The private key never appears in this repository or its configuration.
- **Do NOT use a JWKS endpoint or `issuer-uri`** for key discovery; the key location is
  configured directly.
- Do not hand-roll JWT parsing, signature checking, or any other cryptography. Use the
  resource-server support, as `mneme-core` does.

## Code standards

- Interfaces at behavioural boundaries. Every use case is an interface in `application`
  with a single method, implemented by exactly one class. Controllers and other adapters
  depend on the interface, never the implementation. Ports (`FileStorage`) are interfaces
  declared by the inner layer. Mappers are interfaces too.
- No interfaces over data. Aggregates, value objects, DTOs, and `@ConfigurationProperties`
  types stay concrete. An interface with no behaviour and only one conceivable
  implementation adds noise, not abstraction.
- Implementation naming: when the implementation is characterised by its mechanism, the
  prefix names it (`S3FileStorage`, `MagicByteContentTypeDetector`). When there is nothing
  to say beyond "the one implementation", prefix with `Default` (`DefaultUploadFileUseCase`).
- The domain never calls `Instant.now()`. Time enters as a parameter or via an injected
  `java.time.Clock`.
- This repo knows only its own models. It shares no model libraries with other services.
  Cross-service contracts are expressed as OpenAPI.

## Testing

- JUnit 5 (Spring Boot default). Per the global standards: unit-test the domain and use
  cases without a Spring context; integration-test adapters against real object storage
  via Testcontainers — never against a mocked or in-memory S3.
- The domain's independence from framework and SDK types is asserted, not assumed.

## Out of scope (MVP)

See `PROJECT.md`. In short: no domain knowledge of any kind, no database, no image
processing, no presigned URLs, no AI generation, no direct exposure of the object
storage. Do not build these.