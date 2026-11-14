# API contract (owner side)

This repo owns the contract between the media-api and the ui: an OpenAPI 3.1 spec at `openapi/media-api.yaml`. It's the source of truth. The api's Java interfaces are generated from it at build time, and the ui ([ethannet-ui](https://github.com/ethannet-labs/ethannet-ui)) vendors a copy at a release tag and generates TypeScript types from that. The plan this was built from is [2026-11-12-spring-boot-photos-api](../plans/2026-11-12-spring-boot-photos-api.md). See also [Architecture](../architecture.md) and [Media API](media-api.md).

## How the Java side is generated

`openapi-generator-maven-plugin` (the `spring` generator) runs in `generate-sources` and writes to `target/generated-sources/openapi`, which Maven adds to the compile path:

- `net.ethannet.media.api.<Tag>Api` — one interface per tag, with the route, method and `produces` annotations. Methods are abstract (`skipDefaultInterface`), so a controller that doesn't implement an operation fails to compile.
- `net.ethannet.media.api.model.*` — the request and response models, with Jakarta Validation annotations from the schema's `required` and constraints.

Options are in `pom.xml`: `interfaceOnly`, `useSpringBoot4`, `useJackson3`, `useTags`, `useBeanValidation`, no Swagger annotations (`annotationLibrary`/`documentationProvider` `none`) and no `jackson-databind-nullable` (`openApiNullable=false`). Generated code is never committed or edited.

## Changing the contract

1. Edit `openapi/media-api.yaml` on a feature branch, describing every schema and property (`description`), since both sides' doc comments come from them.
2. `./mvnw verify`. Compilation fails wherever the Java code no longer matches; fix it test-first.
3. Decide the bump (see [Releasing](../releasing.md)): a removed or renamed path, property or enum value, a newly required request field, or a changed type is **breaking** (major; minor while pre-1.0). An added optional field or a new endpoint is minor. Description-only changes are patch.
4. On release, `info.version` in the spec is set to the release version and the merge to `main` is tagged `vX.Y.Z`. The ui then adopts it with `pnpm --filter @ethannet/schema sync:api vX.Y.Z` (see its `docs/features/api-contract.md`).

Rules:

- Additive changes are preferred: add an optional field rather than changing an existing one, so the ui can adopt the release whenever it likes.
- The ui only ever pins released tags, so an unreleased contract change on `dev` doesn't affect it.
- Don't change a response's JSON shape outside the spec (for example with custom Jackson config) — the web-slice tests check exact shapes for this reason.

## Testing

- Compilation checks every controller against its generated interface.
- `@WebMvcTest` tests check status, content type and the exact JSON shape of each response (`HelloControllerTest`).
- The ui's typecheck and e2e suite check the other side once it syncs.
