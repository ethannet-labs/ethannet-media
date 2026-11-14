import type { components } from "./generated/photos-api";

/**
 * Greeting returned by `GET /api/hello`: a human-readable `message` and the ISO
 * 8601 `timestamp` the api built it at.
 *
 * Generated from `openapi/photos-api.yaml`, the source of truth for the
 * photos-api contract. Change the spec, then run `generate:api`; never edit
 * the generated file.
 */
export type HelloResponse = components["schemas"]["HelloResponse"];
