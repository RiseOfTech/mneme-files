# Slice: upload authentication

Branch: `slice/upload-authentication`

## Goal

Make `POST /v1/files` require a valid access token issued by `mneme-auth`, while
`GET /v1/files/{id}` stays public. The service becomes an OAuth2 resource server: it
verifies the token's RS256 signature against `mneme-auth`'s public key, checks expiry and
issuer, and accepts any valid user. It performs no domain authorization — it cannot, and
must not.

This closes the deliberate gap left by `slice/file-upload-download`, where every endpoint
was public.

The token's subject also becomes available for the first time, so this slice records it
on the stored object as audit metadata.

## In scope

### Security configuration

- The resource-server dependency, and a stateless security filter chain.
- `POST /v1/files` requires an authenticated caller. `GET /v1/files/{id}` and the health
  endpoint remain open to anonymous callers.
- A `JwtDecoder` built from `mneme-auth`'s public key, supplied as a PEM file path in
  configuration, with a classpath default for development and an environment override for
  production.
- An explicit issuer validator layered onto the decoder, in addition to the default
  expiry checks.
- Session management is stateless, and CSRF protection is disabled — this is a token
  API with no browser session or cookie.
- Rejected authentication produces **401** with an `application/problem+json` body whose
  `code` is `unauthorized`, matching the error format of every other response.

### Upload

- The token's subject claim is stored on the object as user metadata under the key already
  named in `ARCHITECTURE.md`. It is written only; nothing reads it back, and it appears in
  no response.
- The upload's behaviour is otherwise unchanged: same validation, same status codes, same
  response body.

### Configuration

- The public-key location and the expected issuer, both externally configurable.

## Out of scope

- **`aud` validation.** Deliberately deferred — see `ARCHITECTURE.md`, which records the
  reasoning and the three-step transition plan. Do not add it, and do not add a
  placeholder for it.
- **JWKS or issuer-URI discovery.** The key location is configured directly.
- **Roles, scopes, authorities, or any authority-based rule.** Every valid token is
  equally acceptable. No `hasRole`, no `hasAuthority`, no scope checks, no custom
  `JwtAuthenticationConverter` producing authorities.
- **Domain authorization.** No ownership check of any kind, now or ever.
- **403 responses.** This service makes no authorization decisions, so there is no
  situation that produces one.
- **Protecting `GET /v1/files/{id}`.** Public reads are a product decision, not an
  oversight.
- **`DELETE /v1/files/{id}`.** A later slice.
- **Opaque tokens, token introspection, refresh tokens, or login endpoints.** Token
  issuance belongs to `mneme-auth`.
- **CORS configuration.**
- **Exposing the uploader** in any response body, header, or query.
- **Reading the uploader metadata** anywhere in application code.

## Acceptance criteria

1. `./gradlew build` is green.
2. `POST /v1/files` with a valid token succeeds exactly as before: 201, the same response
   body, the same `Location` header.
3. `POST /v1/files` with no `Authorization` header returns 401 with an
   `application/problem+json` body whose `code` is `unauthorized` — not an empty body.
4. `POST /v1/files` returns 401 for each of: a malformed token, an expired token, a token
   signed by a different key pair, and a token whose issuer is not the configured one.
   Each case is asserted separately.
5. The issuer test fails if the issuer validator is removed. (It is not covered by the
   framework's defaults when the key is configured directly.)
6. `GET /v1/files/{id}` returns the file with no `Authorization` header present.
7. `GET /actuator/health` responds with no token.
8. A file uploaded with a valid token has the token's subject stored as object metadata,
   asserted by reading the object directly from storage in an integration test.
9. No response body, header, or endpoint exposes the uploader value.
10. Uploading a file with an oversized or disallowed payload and a valid token still
    returns 413 / 415 as before — authentication does not change the validation
    behaviour or its error format.
11. A `grep` of the source finds no `aud`/audience validation.
12. A `grep` of the source finds no `hasRole`, `hasAuthority`, `hasAnyRole`, or scope-based
    matcher.
13. A `grep` of the source finds no `jwk-set-uri` and no `issuer-uri` property.
14. A `grep` of the source finds no `BEGIN PRIVATE KEY` outside `src/test/resources`, and
    no private key is referenced by main configuration.
15. A `grep` of the domain package still finds no framework or SDK imports; the
    architecture test still passes.

## Tests

**Issuer validation (plain JUnit)**

- A token with the configured issuer is accepted; one with a different issuer is rejected.

**Web (full context, Testcontainers)**

- Each row of the token matrix in criterion 4, asserting status, `code`, and the
  `application/problem+json` content type.
- The valid-token upload round trip, then an anonymous download of the same id.
- Anonymous `GET` and anonymous health.
- Oversized and disallowed uploads with a valid token, confirming unchanged behaviour.

**Storage (integration)**

- The uploaded object carries the subject as user metadata.

**Test tokens**

- Signed in test code with an RSA key pair kept in `src/test/resources` and used for
  nothing else. The corresponding public key is what the test configuration points at.
  A second, unrelated key pair is needed for the wrong-signature case.

## Notes

- **Adding the security dependency changes the defaults for the whole application.**
  Everything becomes protected unless explicitly permitted. The public download endpoint
  and the health endpoint must be listed, or the previous slice's tests will start failing
  with 401 — which is the expected symptom of forgetting this, not a mystery.
- **CSRF protection must be disabled or `POST` will fail with 403** once security is on
  the classpath, regardless of the token. This is the single most likely way to break a
  working upload in this slice.
- **A 401 has no body by default.** The framework's entry point returns an empty response
  with a `WWW-Authenticate` header. Producing `problem+json` requires an explicit entry
  point; criterion 3 fails without it.
- **The issuer is not validated by default** when the key is configured directly rather
  than discovered from an issuer URI. Only expiry-related claims are checked out of the
  box.
- The public key must be a bare public key in SPKI form (`BEGIN PUBLIC KEY`), not an X.509
  certificate. A certificate will not load.
- **A bad token on a public endpoint is still rejected.** If a caller sends an invalid
  `Authorization` header to `GET /v1/files/{id}`, the bearer-token filter rejects it before
  the endpoint's permit rule applies. This is acceptable — browsers loading an `<img>` send
  no such header — but the download test must be written without one rather than with an
  empty or dummy value.
- Do not hand-roll JWT parsing, decoding, or signature checking anywhere.