# mneme-files — Project

## What this service is

`mneme-files` is the file store of the Mneme system. It sits in front of object storage
and lets other services put files in and get them back by id. It stores bytes. It does
not know what they are for.

It is one of four services:

- `mneme-core` — cards, spaced repetition, the product API
- `mneme-auth` — identity, token issuance
- `mneme-web` — browser client
- `mneme-files` — this service

## Why it exists

Images need somewhere durable to live, and every plausible alternative is worse:

- Storing bytes in `mneme-core` turns a domain service into a file server, and grows its
  database backups by orders of magnitude with data that never changes.
- Storing them on the application's local disk breaks the moment there is a second
  instance, and loses everything when a container is replaced.
- Exposing the object storage to the browser puts storage credentials and the whole
  bucket on the public internet.

A small service that owns files solves all three, and can serve every future consumer —
card images today, avatars and generated images later — without any of them coupling to
the others.

## Why it is deliberately dumb

The service is **domain-agnostic by design**. It must not know that a file is a card
image or a user avatar. That association is owned by whichever service owns the card or
the user, and is stored there as a file id.

This is the property that keeps the service reusable. The moment it learns about cards,
it stops being a file store and becomes a second, worse cards service. Every request to
add domain awareness here is a request to put the logic in the wrong place.

The API is therefore about files, not entities: upload a file, get an id; fetch a file by
id; delete a file by id.

## Responsibilities

**This service is responsible for:**

- Accepting an uploaded file from an authenticated caller, and rejecting it if it is too
  large or not a permitted type
- Assigning each file an unguessable id
- Storing bytes durably and handing them back by id
- Deleting a file by id
- Serving files with correct content types and caching headers

**This service is not responsible for:**

- What a file means or which entity it belongs to
- Who may read a given file (reads are public — see below)
- Producing, transforming, resizing, or converting images
- Cleaning up files whose owning entity was deleted

## Decisions

These were taken with the architecture mentor and are not reopened. Design within them.

### Write requires a token; read is public

Uploads and deletes require a valid access token — the same one `mneme-auth` issues and
`mneme-core` already accepts. This service verifies the **signature** and accepts any
valid user. It performs no domain authorization, because it has no domain knowledge to
authorize against. The purpose of write auth is to stop anonymous strangers filling the
store, nothing more.

Downloads are **public by id**, with no token, so that an `<img src>` in the browser
works directly. This follows a decision already taken in `mneme-core`: card images are
not siloed.

### File ids are the access control

Because reads are unauthenticated, an unguessable id is the only thing protecting a file.
Ids are therefore random UUIDs — never sequential, never time-ordered. This is a security
property of the system, not an implementation detail.

The privacy this provides is real but bounded: anyone who learns an id can read that file
forever. That is an accepted trade-off, consistent with the rest of the product.

### No database

The service has no relational database. Everything it needs to know about a file — its
content type, size, and ETag — is stored on the object itself and read back from the
storage. There is no per-user aggregate to maintain, because there are no quotas.

### Limits

Uploads are capped at **5 MB** and restricted to **JPEG, PNG, and WebP**.

The cap is a guard against mistakes and abuse, not a working ceiling: clients resize
before uploading. GIF is excluded (animation is video-sized in practice); SVG is excluded
deliberately and permanently — it is an executable document, and serving one from a
public endpoint is a cross-site scripting vector.

The declared content type of an upload is not trusted. The type is determined from the
file's own bytes.

## API surface

Full contract per slice; this is the shape.

- `POST /v1/files` — multipart upload, requires a token, returns the new file id
- `GET /v1/files/{id}` — returns the bytes, public, cacheable
- `DELETE /v1/files/{id}` — requires a token; deleting an unknown id is a no-op, not an
  error

Endpoints are versioned under `/v1`. Errors use `application/problem+json` with a `code`
property, matching `mneme-core`'s conventions.

## How consumers use it

For context only — none of this is implemented here.

When a user attaches an image to a card, `mneme-core` uploads it here, receives an id,
and stores that id on the card. When it returns the card, it exposes a URL pointing at
this service's public download endpoint, and the browser loads the image directly. Later,
`mneme-auth` may store avatars the same way.

This service is unaware of all of it. It stored a file and will hand it back.

## Out of scope

Not "not yet" unless stated — these are things not to build.

- **Domain knowledge of any kind.** No cards, no users, no ownership, no per-entity
  endpoints.
- **File↔entity relationships.** Stored by the owning service, never here.
- **Domain authorization.** Signature validity only.
- **A database.** No tables, no migrations, no quotas, no listings.
- **Image processing.** No server-side resizing, thumbnails, or format conversion.
  Clients resize before upload. *(A named later concern.)*
- **AI image generation.** A Phase 2 concern of a different service.
- **Presigned URLs or direct-to-storage uploads.** This service serves the bytes.
  *(A named upgrade path for later scale.)*
- **Public exposure of the object storage.** Only this service talks to it.
- **Orphan cleanup.** Files outlive the entities that referenced them. Reclaiming them
  requires knowing what referenced them, which this service cannot know. If it is ever
  worth doing, it is the owning service's job to ask for a delete.

## Status

Design stage. No slice implemented yet. First slice: authenticated upload plus public
download by id, against real object storage in Testcontainers.