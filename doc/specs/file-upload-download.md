# Slice: file upload and download

Branch: `slice/file-upload-download`

## Goal

Deliver the service's core capability end to end: a file can be uploaded over HTTP,
stored in object storage, and fetched back by its id. Uploads are validated against a
size cap and a content-type allowlist. This slice proves the storage port, its S3
adapter, and the streaming paths in both directions against real object storage.

Authentication is **not** part of this slice — every endpoint here is public. The next
slice (`slice/upload-authentication`) makes `POST` require a token. This is a deliberate
ordering, not an oversight.

Prerequisite: `chore/init` has produced a running Spring Boot skeleton.

## In scope

### Domain

- `FileId` — a value type over `UUID`, with a generator using `UUID.randomUUID()`.
- `ContentType` — a value type over the permitted MIME string, rejecting anything not on
  the configured allowlist at construction.
- `FileContent` — the data, size, and content type of a file being stored.
- `StoredFile` — the id, content type, size, ETag, and data of a file being served.
- A domain exception for a file exceeding the size limit, and one for a disallowed
  content type.

### Storage port

- `FileStorage` with exactly two operations in this slice: store a file under a given id,
  and load a file by id returning an empty optional when absent.
- Byte payloads cross the port as `java.io.InputStream`.
- The port documents that the caller closes the returned stream.

### Storage adapter

- An S3 implementation of the port using AWS SDK v2, configured for path-style access
  with static credentials from configuration.
- The object key is the id's string form: flat, no prefix, no extension.
- The detected content type is stored as the object's content type.
- A missing object maps to an empty optional; other SDK failures are wrapped in a
  service-level exception at the adapter boundary.

### Content-type detection

- Detection from the file's leading bytes for JPEG, PNG, and WebP. No match is a
  rejection.
- The client-declared content type is ignored entirely.

### Application

- A use case for uploading a file: detect the type, validate size and type, generate an
  id, store, and return the id with its content type and size.
- A use case for fetching a file by id.

### Web

- `POST /v1/files` — `multipart/form-data`, one file part. On success returns **201** with
  a body containing the file id, content type, and size, and a `Location` header pointing
  at the file's download URL.
- `GET /v1/files/{id}` — returns the bytes with `Content-Type` and `Content-Length` from
  the stored object, the storage `ETag`, and
  `Cache-Control: public, max-age=31536000, immutable`. The body is streamed, never
  buffered whole in memory.
- Errors as `application/problem+json` with a `code` property:

  | Situation | Status | `code` |
    |---|---|---|
  | Unknown file id | 404 | `file_not_found` |
  | Upload above the size cap | 413 | `file_too_large` |
  | Detected type not on the allowlist | 415 | `unsupported_content_type` |
  | Malformed or missing multipart body | 400 | `invalid_request` |

### Configuration

- Storage endpoint, bucket, region, and credentials.
- Maximum upload size and the allowed content-type list, bound as typed configuration and
  validated at startup.
- Multipart limits aligned with the configured maximum.
- The container's maximum swallow size raised so that an oversized upload receives the
  413 response rather than a dropped connection.

## Out of scope

Do not build any of the following in this slice, even where it seems a natural addition.

- **Any authentication or authorization.** Do not add Spring Security, a resource server,
  a `SecurityFilterChain`, or JWT handling of any kind. The endpoints are public in this
  slice by design.
- **`DELETE /v1/files/{id}`** and any delete operation on the port or adapter.
- **Additional port operations.** No `exists`, `list`, `copy`, `move`, `head`, or batch
  variants.
- **Bucket creation by the application.** The bucket is provisioned by the environment and
  by test setup. Do not call `CreateBucket` from application code or from a startup hook.
- **Any database.** No Spring Data, no JDBC, no driver, no Flyway, no entities.
- **Apache Tika** or any general content-detection library.
- **Conditional requests** (`If-None-Match`, 304), **range requests**, and multipart
  upload to S3.
- **Image processing** of any kind: no resizing, thumbnails, or conversion.
- **Presigned URLs** or any direct-to-storage path.
- **CORS configuration.**
- **Domain concepts.** No cards, users, owners, or ownership anywhere in names, types,
  packages, or paths.
- **The uploader audit metadata** (`x-amz-meta-uploader`). It carries the token subject,
  which does not exist until the authentication slice.
- **Kotlin source files.** Java only; the Kotlin DSL is for build scripts.

## Acceptance criteria

1. `./gradlew build` is green.
2. Uploading a JPEG, a PNG, and a WebP each returns 201 with a body containing a
   well-formed UUID id, the detected content type, and the byte size, plus a `Location`
   header addressing that id.
3. Fetching a previously uploaded file by its id returns 200, the same bytes, the stored
   content type, the correct `Content-Length`, a non-empty `ETag`, and
   `Cache-Control: public, max-age=31536000, immutable`.
4. Fetching a well-formed but unknown UUID returns 404 with an
   `application/problem+json` body whose `code` is `file_not_found`.
5. Fetching a malformed id returns 400 and does not reach the storage.
6. Uploading a file above the configured cap returns 413 with `code` `file_too_large`,
   with a complete response body — not a connection reset.
7. Uploading a file whose leading bytes match no allowed type returns 415 with `code`
   `unsupported_content_type`, including when the declared content type claims an allowed
   one.
8. Uploading a JPEG whose declared content type is `application/octet-stream` succeeds and
   is stored as the detected image type — the declared value has no effect on the outcome.
9. Every id returned by two uploads of identical bytes is distinct, and both files are
   independently retrievable.
10. The generated ids are version 4 UUIDs. A `grep` of the source finds no UUIDv7, ULID,
    or other time-ordered or sequential id generation.
11. A `grep` of the domain package finds no import of `org.springframework`,
    `software.amazon`, or `jakarta`. This is also asserted by a test.
12. A `grep` of the source finds no `CreateBucket` call outside test code.
13. A `grep` of the source finds no reference to Spring Security, and the build file
    declares no security dependency.
14. The download path does not read the file into a `byte[]` or `ByteArrayOutputStream`;
    the response streams from the storage.
15. The object key stored for a file equals the string form of its id, with no prefix,
    directory segment, or extension.

## Tests

**Domain (plain JUnit, no Spring context)**

- `ContentType` rejects values outside the allowlist and accepts each permitted one.
- Size validation rejects a size above the limit and accepts one at the boundary.
- `FileId` generation produces distinct version 4 values.

**Content-type detection (plain JUnit)**

- Real JPEG, PNG, and WebP byte fixtures are each detected correctly.
- A text file, an SVG document, and a truncated header are each rejected.
- A JPEG's detection is unaffected by any declared content type.

**Use cases (plain JUnit, storage port stubbed)**

- Upload validates before storing: a rejected file results in zero interactions with the
  storage port.
- Fetching an id the storage does not have produces the not-found outcome.

**Storage adapter (integration, Testcontainers, real object storage)**

- A stored file is loaded back with identical bytes, content type, and size.
- Loading an absent id yields an empty optional.
- The object is stored under the expected flat key.

**Web (full context, Testcontainers)**

- The full upload-then-download round trip over HTTP for each allowed type.
- Each error case in the table above, asserting status, `code`, and content type of the
  problem body.
- The oversized-upload case asserts a complete 413 response body is received, not a
  transport failure.

Object storage is real in every integration test. Do not mock the SDK or substitute an
in-memory S3.

## Notes

- The bucket is created in test setup, not by the application.
- Spring buffers the multipart body before the controller runs, so the upload stream can
  be read once for signature detection and again for storage without `mark`/`reset`
  handling, and the exact size is available from the multipart file.
- `MaxUploadSizeExceededException` needs explicit handling to produce the `problem+json`
  body; without it the framework's default response is returned instead.
- Raising the container's swallow size is what makes criterion 6 observable. If it is left
  at its default, the service will send a correct 413 that the client never sees.
- Path-style access must be enabled on the S3 client. Without it the SDK resolves
  `bucket.host` against the non-AWS endpoint and every call fails.
- Test fixtures must be real image files. A file containing the text "fake jpeg" will not
  pass detection, which is the point.