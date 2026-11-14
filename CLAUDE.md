# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

The ethannet **photos-api**: a Spring Boot 4 service in Java 25, built with Maven (through the wrapper, `./mvnw`) and backed by Postgres, on port 4001. It's a plain Maven project at the repo root (no Node tooling). It owns the OpenAPI contract in `openapi/photos-api.yaml`. The Next.js UI that calls it lives in a separate repo, [ethannet-ui](https://github.com/Ethan-S-Evans/ethannet-ui), checked out locally at `../ethannet-ui`. See [docs/architecture.md](docs/architecture.md) for the rationale behind the setup, [docs/features/photos-api.md](docs/features/photos-api.md) for running and testing it, [docs/features/api-contract.md](docs/features/api-contract.md) for the contract workflow, and [docs/releasing.md](docs/releasing.md) for the branch and release workflow; implementation plans live in [docs/plans/](docs/plans/).

## Commands

Needs JDK 25 on `JAVA_HOME`, Podman, and `docker-compose` (which `podman compose` delegates to). Export `DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock` and `TESTCONTAINERS_RYUK_DISABLED=true` (the Podman socket is enabled with `systemctl --user enable --now podman.socket`).

**Localhost only**: everything binds to `127.0.0.1` (api, compose Postgres via `network_mode: host`, Testcontainers' published ports, and Ryuk off), so nothing is reachable from the network. Keep it that way: never publish a container port without `127.0.0.1:`, and never default a listen address to `0.0.0.0`. See [docs/features/photos-api.md](docs/features/photos-api.md#localhost-only).

- `podman compose up -d postgres` / `podman compose down` — local Postgres from `compose.yaml`
- `./mvnw spring-boot:run` — run the api against the compose Postgres
- `./mvnw spring-boot:test-run` — run the api against a throwaway Testcontainers Postgres (no compose needed; used for the ui's e2e suite)
- `./mvnw test` — all tests; `./mvnw test -Dtest=HelloControllerTest` for one class, `-Dtest='HelloControllerTest#rejectsOtherOrigins'` for one method
- `./mvnw verify` — tests, `spotless:check` and the jar build. This is the "everything passes" check
- `./mvnw spotless:apply` — format the Java code
- `./mvnw generate-sources` — regenerate the contract interfaces and models into `target/generated-sources/openapi`

## Architecture

- **Contract first.** `openapi/photos-api.yaml` is the source of truth. `openapi-generator-maven-plugin` (`spring` generator, `interfaceOnly`, `skipDefaultInterface`, `useSpringBoot4`, `useJackson3`, `useTags`) generates one interface per tag (`net.ethannet.photos.api.HelloApi`) and the models (`net.ethannet.photos.api.model.*`) at build time; generated code is never committed or edited. Controllers implement the generated interfaces, so a drifting route or shape fails to compile. The ui vendors the spec at a release tag, so contract changes reach it only when a release is tagged. See [docs/features/api-contract.md](docs/features/api-contract.md).
- **Feature packages** under `net.ethannet.photos` (`hello/`, later `photos/`, `albums/`...), each holding its controller, services or builders, and later entities and repositories. The flow is controller → service → repository: controllers only translate HTTP and delegate. Response-building logic is extracted into pure classes (e.g. `HelloResponseBuilder`, with an injected `Clock`) so plain JUnit can test it. Cross-cutting config lives in `config/` (`ClockConfig`, `CorsConfig` + `CorsProperties`).
- **Configuration** is `src/main/resources/application.yml`; every environment-specific value reads an env var with a local default (`SERVER_ADDRESS` (default `127.0.0.1`), `PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS`). Typed settings use `@ConfigurationProperties` records. CORS allows only `app.cors.allowed-origins` on `/api/**`.
- **Persistence**: Flyway owns the schema (`src/main/resources/db/migration/V<n>__<description>.sql`, no tables yet); Hibernate runs with `ddl-auto: validate` and `open-in-view: false`. Only `/actuator/health` is exposed, and it includes the database.
- **Code style**: google-java-format via Spotless (`spotless:check` runs in `verify`). Tests mirror the main package layout.

## Development Principles

### Branching (Git Flow)

All changes are made on a feature branch. Never commit directly to `main` or `dev`.

- **`main`**: released code only. It changes only when a release is merged in from `dev`.
- **`dev`**: the integration branch. Feature branches branch off it and merge back into it.
- **Feature branches**: create them from `dev` with names like `feature/<short-description>`, and merge them back into `dev` when the work is done and `./mvnw verify` passes.
- **Releases**: when the user decides to cut a release, `dev` is merged into `main`. Only do this when the user asks.

### Versioning

The photos-api has one [semver](https://semver.org) version (`MAJOR.MINOR.PATCH`): `<version>` in `pom.xml`, which must equal `info.version` in `openapi/photos-api.yaml` at each release. `dev` carries the next version as `-SNAPSHOT`. Each release to `main` is tagged `vX.Y.Z`. There are no changeset files; the bump is chosen at release time from what merged since the last tag, so merge messages should say what changed, especially in the contract. The ui repo versions itself independently. See [docs/releasing.md](docs/releasing.md).

- **Bump types**:
  - **major**: a breaking change to the contract in `openapi/photos-api.yaml` (removed or renamed paths, properties or enum values; changed types; newly required request fields), or a large overhaul.
  - **minor**: a new endpoint or feature that doesn't break anything.
  - **patch**: a bug fix or a small internal change.
- **Pre-1.0**: the version stays on `0.y.z` until the user decides it's ready for `1.0.0`. Until then, breaking changes bump the minor version.
- **Releasing**: only when the user asks.

### External dependencies

Every third-party dependency — Maven dependencies **and** Maven plugins — must be free, permissively licensed open source. Check its license (the `<licenses>` block in its published POM, or its parent POM, or its repository) before adding it.

- **Allowed**: MIT, Apache-2.0, ISC, BSD-2-Clause, BSD-3-Clause, 0BSD, and EPL-2.0. EDL-1.0 (the Eclipse Distribution License) is the BSD-3-Clause text and counts as BSD-3-Clause.
  - **EPL-2.0 only for unmodified dependencies.** Use EPL-2.0 libraries as published (e.g. JUnit, AspectJ Weaver, Logback). Never copy their source into this repo or patch them: changed EPL files must be published under EPL. If a jar or container image that bundles them is ever distributed, keep their license notices in it.
  - **Dual-licensed dependencies** are allowed when one of their options is on this list. Treat them as licensed under that option (e.g. Jakarta Annotations, "EPL-2.0 or GPL-2.0 with Classpath Exception", is used under EPL-2.0; Logback, "EPL-2.0 or LGPL-2.1", under EPL-2.0).
- **Not allowed without the user's explicit approval**: copyleft licenses (GPL, LGPL, AGPL, MPL, EUPL, EPL-1.0), "source-available" or non-commercial licenses (BSL, SSPL, Commons Clause, CC-BY-NC), packages with no license, and anything that requires a paid license (e.g. Flyway's paid editions; use only the Apache-2.0 community artifacts).
- If a dependency's license isn't on the allowed list, or you're unsure about it, ask before adding it.
- Pin plugin versions as properties in `pom.xml`. Dependency versions come from the Spring Boot parent's dependency management wherever it manages them.

### SOLID design

All new code follows SOLID. Dependency inversion matters most here.

- **Single Responsibility**: each class has one reason to change. Controllers translate HTTP and delegate; services hold business logic; repositories hold persistence; builders hold pure response logic.
- **Open/Closed**: extend behavior by adding code (new feature packages, new beans, new contract operations), not by editing working code to add special cases.
- **Liskov Substitution**: any implementation of an interface (including the generated API interfaces) must work wherever that interface is expected, with no surprise behavior, extra preconditions, or narrowed outputs.
- **Interface Segregation**: keep interfaces small and specific; one generated interface per contract tag, and services that expose only what callers use.
- **Dependency Inversion**: high-level code depends on abstractions. Inject dependencies (repositories, clients, config, `Clock`) through **constructors** — never field `@Autowired` or setter injection — so they can be swapped and mocked in tests. Interfaces for external systems (e.g. a future `StorageService` for S3) live with the feature that needs them. Shared API contracts belong in `openapi/photos-api.yaml`.

### Test-driven development

TDD is the default workflow. Write the failing test first, make it pass with the simplest implementation, then refactor.

- **Services, builders and other logic**: plain JUnit 5 + AssertJ tests with no Spring context; inject fakes or fixed values (e.g. `Clock.fixed`).
- **Controllers**: a `@WebMvcTest` per controller that checks status, content type and exact JSON shape against the contract, mocking collaborators with `@MockitoBean`.
- **Persistence and wiring**: `@DataJpaTest` or `@SpringBootTest` with `@Import(TestcontainersConfiguration.class)`, against a real Postgres. Never H2.
- No feature is done until its tests exist and `./mvnw verify` passes. A change that the ui renders also needs the ui repo's e2e suite to pass against it.

### Documentation

Every new feature and every change ships with documentation. There is no fixed documentation style yet, so choose the clearest form for each case, but don't skip it. A feature isn't done until its docs exist and match the code.

- **Contract**: describe every operation, schema and property in `openapi/photos-api.yaml`; those descriptions become doc comments on both sides.
- **Code**: add Javadoc to public classes and methods, especially where intent or constraints aren't obvious from the signature.
- **Workflows**: document dev, build, test, and release workflows, and any setup steps, in `docs/` (or here, if they belong in this overview). Update them when a workflow changes.
- **Testing and usability**: record how to test a feature and how it's meant to be used: the flows it supports, how to run or reproduce it, and the edge cases covered.
- **Features**: each feature gets a page in `docs/features/` (or a section in [docs/features/photos-api.md](docs/features/photos-api.md) while it's small) covering what it does, how to change it, its flows and edge cases, and how to test it.
- **Plans**: save every approved implementation plan to `docs/plans/` as `YYYY-MM-DD-short-slug.md`, before or alongside the implementation. Link the plan to the feature docs it produces and link those docs back to the plan. If the build ends up differing from the plan, add an "As built" section noting the differences. Plans from before the November 2026 split describe the old TypeScript monorepo and are kept as history.
- Update existing docs in the same change that makes them out of date.
- **Obsidian vault**: the repo is meant to be opened as an Obsidian vault for reading the docs. Write all written documentation as Markdown (`.md`) files, and link related docs to each other wherever it helps a reader navigate. Use relative Markdown links (`[Architecture](docs/architecture.md)`) so the links work both in Obsidian and on GitHub; link to the ui repo with GitHub URLs.
