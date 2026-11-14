# ethannet-media

The ethannet **photos-api**: a Spring Boot 4 service (Java 25, Maven) backed by Postgres. The UI that calls it lives in its own repo, [ethannet-ui](https://github.com/Ethan-S-Evans/ethannet-ui).

## Layout

- [openapi/photos-api.yaml](openapi/photos-api.yaml) — the API contract, source of truth for both repos
- [src/main/java/net/ethannet/photos](src/main/java/net/ethannet/photos) — the application, in feature packages
- [src/main/resources](src/main/resources) — `application.yml` and Flyway migrations
- [compose.yaml](compose.yaml) — local Postgres
- [docs](docs) — documentation, including plans

See [docs/architecture.md](docs/architecture.md) for how the pieces fit together, [docs/features/photos-api.md](docs/features/photos-api.md) for running, configuring and testing the service, [docs/features/api-contract.md](docs/features/api-contract.md) for the contract workflow, and [docs/releasing.md](docs/releasing.md) for branches, versions and releases.

## Getting started

Needs JDK 25 and Podman (setup in [docs/features/photos-api.md](docs/features/photos-api.md#prerequisites)).

```
podman compose up -d postgres
./mvnw spring-boot:run
```

Or, with a throwaway Testcontainers Postgres and no compose: `./mvnw spring-boot:test-run`. The api listens on http://localhost:4001.

## Commands

| Command | Description |
| --- | --- |
| `./mvnw spring-boot:run` | Run the api against the compose Postgres |
| `./mvnw spring-boot:test-run` | Run the api against a throwaway Testcontainers Postgres |
| `./mvnw test` | Run all tests (unit, web slice, Testcontainers) |
| `./mvnw test -Dtest=HelloControllerTest` | Run one test class |
| `./mvnw verify` | Tests, formatting check and jar build |
| `./mvnw spotless:apply` | Format the Java code |
| `podman compose up -d postgres` / `podman compose down` | Start / stop local Postgres |

## License

Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE). Third-party dependencies keep their own licenses; see the license policy in [CLAUDE.md](CLAUDE.md#external-dependencies).
