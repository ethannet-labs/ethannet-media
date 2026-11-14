# Media API

The Spring Boot service behind the ethannet ui. Today it serves one endpoint, `GET /api/hello`, which is the first vertical slice through the contract, the api and the ui. The plan this was built from is [2026-11-12-spring-boot-photos-api](../plans/2026-11-12-spring-boot-photos-api.md). For how it fits with the ui, see [Architecture](../architecture.md); for the contract, see [API contract](api-contract.md).

## Prerequisites

- **JDK 25.** With a user-local install, point `JAVA_HOME` at it, e.g. `export JAVA_HOME="$HOME/.jdks/jdk-25.0.5+8"` and `PATH="$JAVA_HOME/bin:$PATH"`.
- **Podman**, with its user API socket enabled for Testcontainers and docker-compose:

  ```
  systemctl --user enable --now podman.socket
  export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
  export TESTCONTAINERS_RYUK_DISABLED=true
  ```

  Put the two exports in your shell profile. Ryuk, Testcontainers' cleanup container, is disabled because it publishes a port on every network interface (see [Localhost only](#localhost-only)). Without it, Testcontainers stops its containers from a JVM shutdown hook. Only a killed test JVM leaves one behind; remove it with `podman ps` / `podman rm -f`.
- **docker-compose** (Apache-2.0), which `podman compose` delegates to. Install the standalone binary from the [docker/compose releases](https://github.com/docker/compose/releases) into `~/.local/bin/docker-compose`, checking it against the release's `.sha256`. It talks to Podman through `DOCKER_HOST`. Only needed for the `compose.yaml` Postgres; `spring-boot:test-run` below works without it.

## Localhost only

Nothing the media-api runs is reachable from other machines, which matters on shared networks such as public Wi-Fi:

| What | Listens on | How |
| --- | --- | --- |
| media-api | `127.0.0.1:4001` | `server.address: ${SERVER_ADDRESS:127.0.0.1}` in `application.yml` |
| compose Postgres | `127.0.0.1:5432` | `network_mode: host` plus `listen_addresses=127.0.0.1`. No port is published and no container network is created. |
| Testcontainers Postgres | `127.0.0.1:<random>` | `TestcontainersConfiguration` binds its published port to `127.0.0.1` |
| Ryuk | not started | `TESTCONTAINERS_RYUK_DISABLED=true` |

The ui repo's dev server and Storybook bind to `127.0.0.1` too. Check with `ss -ltn`: every dev port should show `127.0.0.1` (or `[::ffff:127.0.0.1]`), never `0.0.0.0`, `*` or `[::]`. Set `SERVER_ADDRESS=0.0.0.0` only where the api must be reachable, such as inside a future container image.

The compose Postgres uses the host's network, which also sidesteps a Podman problem on networks that use all of `10.0.0.0/8` (some public Wi-Fi does). There, Podman can't find a free subnet for a compose network (`could not find free subnet from subnet pools`), because its default pools are all inside that range.

## Running it

Against the compose Postgres (data persists in the `media-db` volume):

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
| `SERVER_ADDRESS` | `127.0.0.1` | Address to listen on (loopback only by default) |
| `PORT` | `4001` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/media` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `media` / `media` | Database credentials (match `compose.yaml`) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated browser origins allowed to call `/api/**` |

Only `/actuator/health` is exposed from Actuator.

## Package structure

```
net.ethannet.media
  MediaApiApplication
  config/      ClockConfig (system UTC Clock bean), CorsConfig + CorsProperties (app.cors.*)
  hello/       HelloController (implements generated HelloApi) → HelloResponseBuilder
  api/         generated from openapi/media-api.yaml (target/generated-sources, not committed)
```

Each feature gets its own package. Within it, the flow is **controller → service → repository**: the controller implements the generated interface and only translates HTTP, services hold the logic, and repositories (Spring Data JPA) hold persistence. Pure logic such as `HelloResponseBuilder` takes its dependencies (here a `Clock`) through the constructor, so it can be tested with no Spring. All injection is constructor injection, never field `@Autowired`.

## Adding an endpoint

1. Add the path and schemas to `openapi/media-api.yaml` (see [API contract](api-contract.md#changing-the-contract)). Give each operation an `operationId` and a `tags` entry: the tag names the generated interface (`photos` → `PhotosApi`).
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
| Integration (`@SpringBootTest` + Testcontainers) | `MediaApiSmokeTest` | The whole app against real Postgres 18: Flyway validates with nothing pending, `/actuator/health` is `UP`, and `/api/hello` serves the real greeting. |

End to end, the ui repo's Playwright suite renders the greeting from a running media-api (`tests/home.spec.ts` in [ethannet-ui](https://github.com/Ethan-S-Evans/ethannet-ui)).
