# Architecture

## Two repos

ethannet is split across two repos, one language each:

| Repo | What it is | Port |
| --- | --- | --- |
| **ethannet** (this repo) | The Spring Boot **photos-api** and its Postgres database. Java 25, Maven. It owns the API contract. | photos-api 4001, Postgres 5432 |
| [**ethannet-ui**](https://github.com/Ethan-S-Evans/ethannet-ui) | The Next.js UI, Storybook and the Playwright e2e suite. TypeScript, pnpm. | ui 3000, Storybook 6006 |

Until November 2026 this repo was an all-TypeScript pnpm monorepo, with an Express api, a media-server and the UI together. It was split so that each repo has one toolchain, its own version and release cycle, and its own CI. The backend kept this repo, since it's the long-lived service the photo features grow into, and the UI moved to `ethannet-ui` with its history. The old TypeScript code is still in this repo's git history. The plan behind the split is [2026-11-12-spring-boot-photos-api](plans/2026-11-12-spring-boot-photos-api.md).

## Why Spring Boot and Postgres

The photo app needs a real backend with relational data: photos, albums, members and share links. Spring Boot with JPA, Flyway and Postgres is the standard enterprise stack for that, and it puts Java and SQL experience in the portfolio next to the TypeScript and React work in the UI.

## Contract first

```
openapi/photos-api.yaml  (this repo, source of truth)
  ├─ openapi-generator (Maven, at build time) → Java interfaces + models in target/generated-sources
  │     └─ controllers implement them, so a route or shape that drifts fails to compile
  └─ released as tag vX.Y.Z → ethannet-ui runs `sync:api vX.Y.Z`
        └─ vendored copy → openapi-typescript → TS types the ui imports
```

See [API contract](features/api-contract.md) for the workflow and the rules for changing it.

## Layout

- `openapi/photos-api.yaml` — the API contract.
- `src/main/java/net/ethannet/photos/` — the application, in feature packages (`hello/`, later `photos/`, `albums/`...). Each feature holds its controller, its services or builders, and later its entities and repositories. Cross-cutting configuration lives in `config/`. See [Photos API](features/photos-api.md).
- `src/main/resources/application.yml` — configuration, with every environment-specific value read from an env var with a local default.
- `src/main/resources/db/migration/` — Flyway migrations (`V1__description.sql`, ...). Flyway owns the schema; Hibernate runs with `ddl-auto: validate`.
- `src/test/java/...` — tests, mirroring the main packages. `TestcontainersConfiguration` provides a throwaway Postgres, and `TestPhotosApiApplication` runs the app against it.
- `compose.yaml` — local Postgres for development.
- `docs/` — this documentation: [plans](plans) and [features](features).

## Runtime picture

Browser → Next.js ui (`ethannet-ui`, 3000) → photos-api (4001) → Postgres (5432). The ui's server components call `/api/*`; CORS allows the ui's origin for browser calls. `/actuator/health` reports `UP` only when the database is reachable, and the ui's e2e suite waits on it.

Later plans add S3 for photo bytes (the browser uploads directly through presigned URLs) and an async worker for thumbnails and EXIF.

## Tooling

- **Maven wrapper** (`./mvnw`) — no global Maven install. It downloads the Maven version pinned in `.mvn/wrapper/maven-wrapper.properties`.
- **openapi-generator-maven-plugin** — generates interfaces (`interfaceOnly`, `skipDefaultInterface`) and models in `generate-sources`.
- **Spotless + google-java-format** — formatting. `spotless:check` runs in `verify`.
- **JUnit 5, Spring's test slices, Testcontainers** — see [Photos API](features/photos-api.md#testing).
- **Podman** — the container runtime, for `compose.yaml` and for Testcontainers through Podman's Docker-compatible socket.
