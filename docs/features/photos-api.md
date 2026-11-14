# Photos API

The Spring Boot service behind the ethannet ui. Today it serves one endpoint, `GET /api/hello`, which is the first vertical slice through the contract, the api and the ui. The plan this was built from is [2026-11-12-spring-boot-photos-api](../plans/2026-11-12-spring-boot-photos-api.md). For how it fits with the ui, see [Architecture](../architecture.md); for the contract, see [API contract](api-contract.md).

## Prerequisites

- **JDK 25.** With a user-local install, point `JAVA_HOME` at it, e.g. `export JAVA_HOME="$HOME/.jdks/jdk-25.0.5+8"` and `PATH="$JAVA_HOME/bin:$PATH"`.
- **Podman**, with its user API socket enabled for Testcontainers:

  ```
  systemctl --user enable --now podman.socket
  export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
  export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$XDG_RUNTIME_DIR/podman/podman.sock
  ```

  Put the two exports in your shell profile. The socket override lets Testcontainers' Ryuk cleanup container mount the Podman socket. If Ryuk still fails on your machine, set `TESTCONTAINERS_RYUK_DISABLED=true` (containers are then left behind if a test JVM crashes).
- **docker-compose** (Apache-2.0), which `podman compose` delegates to. Install the standalone binary from the [docker/compose releases](https://github.com/docker/compose/releases) into `~/.local/bin/docker-compose`, checking it against the release's `.sha256`. It talks to Podman through `DOCKER_HOST`. Only needed for the `compose.yaml` Postgres; `spring-boot:test-run` below works without it.

### Troubleshooting

- **`could not find free subnet from subnet pools`** from `podman compose up`: Podman's default pools for new networks are all inside `10.0.0.0/8`, so a host network using all of `10.0.0.0/8` (some Wi-Fi networks do) leaves none free. Point Podman at an unused range in `~/.config/containers/containers.conf`:

  ```toml
  [network]
  default_subnet_pools = [
    {"base" = "172.30.0.0/16", "size" = 24},
  ]
  ```

  Testcontainers isn't affected, since it uses Podman's existing default network.

## Running it

Against the compose Postgres (data persists in the `photos-db` volume):

```
podman compose up -d postgres
./mvnw spring-boot:run
```

Or with a throwaway Testcontainers Postgres, with no compose needed (data is lost on exit):

```
./mvnw spring-boot:test-run
```

Either way the api listens on http://localhost:4001. Check it with `curl localhost:4001/actuator/health` and `curl localhost:4001/api/hello`.

## Configuration

All set in `src/main/resources/application.yml`, each with an env var override:

| Env var | Default | What |
| --- | --- | --- |
| `PORT` | `4001` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/photos` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `photos` / `photos` | Database credentials (match `compose.yaml`) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated browser origins allowed to call `/api/**` |

Only `/actuator/health` is exposed from Actuator.

## Package structure

```
net.ethannet.photos
  PhotosApiApplication
  config/      ClockConfig (system UTC Clock bean), CorsConfig + CorsProperties (app.cors.*)
  hello/       HelloController (implements generated HelloApi) → HelloResponseBuilder
  api/         generated from openapi/photos-api.yaml (target/generated-sources, not committed)
```

Each feature gets its own package. Within it, the flow is **controller → service → repository**: the controller implements the generated interface and only translates HTTP, services hold the logic, and repositories (Spring Data JPA) hold persistence. Pure logic such as `HelloResponseBuilder` takes its dependencies (here a `Clock`) through the constructor, so it can be tested with no Spring. All injection is constructor injection, never field `@Autowired`.

## Adding an endpoint

1. Add the path and schemas to `openapi/photos-api.yaml` (see [API contract](api-contract.md#changing-the-contract)). Give each operation an `operationId` and a `tags` entry: the tag names the generated interface (`photos` → `PhotosApi`).
2. `./mvnw generate-sources` to see the generated interface and models in `target/generated-sources/openapi`.
3. Write the failing tests: a plain JUnit test for the service or builder, and a `@WebMvcTest` for the controller.
4. Implement the controller (`implements PhotosApi`) and the service.
5. If it needs tables, add a Flyway migration and a Testcontainers-backed test of the repository (`@DataJpaTest` with `@Import(TestcontainersConfiguration.class)`).
6. `./mvnw verify`, then update this page and the contract doc.

## Testing

`./mvnw test` runs everything; `./mvnw verify` also runs the Spotless check and packages the jar. One class: `./mvnw test -Dtest=HelloControllerTest`.

| Level | Example | What it covers |
| --- | --- | --- |
| Unit (JUnit 5 + AssertJ) | `HelloResponseBuilderTest` | Pure logic with a fixed `Clock`: the message, and a UTC timestamp even when the clock's zone isn't UTC. |
| Web slice (`@WebMvcTest`) | `HelloControllerTest` | Status, content type and exact JSON shape against the contract (builder mocked with `@MockitoBean`), plus CORS: the configured origin is allowed and others get `403`. |
| Integration (`@SpringBootTest` + Testcontainers) | `PhotosApiSmokeTest` | The whole app against real Postgres 18: Flyway validates with nothing pending, `/actuator/health` is `UP`, and `/api/hello` serves the real greeting. |

End to end, the ui repo's Playwright suite renders the greeting from a running photos-api (`tests/home.spec.ts` in [ethannet-ui](https://github.com/Ethan-S-Evans/ethannet-ui)).
