# Branching, versioning and releasing

How changes move from a feature branch to a release on `main`, and how version numbers are chosen. The rules are summarised in [CLAUDE.md](../CLAUDE.md); this page is the step-by-step version. For how the code fits together, see [Architecture](architecture.md).

## Branches

The repo uses Git Flow with three kinds of branch:

| Branch | Purpose | Who merges into it |
| --- | --- | --- |
| `main` | Released code only. Every commit on `main` is a release, tagged `vX.Y.Z`. | `dev`, at release time |
| `dev` | Integration branch. Always the latest finished work. | Feature branches |
| `feature/<short-description>` | One piece of work. | Nobody; it's merged into `dev` |

Nothing is committed directly to `main` or `dev`.

## Versions

The media-api has one [semver](https://semver.org) version, `MAJOR.MINOR.PATCH`, in two places that must match at release:

- `<version>` in `pom.xml` (the jar is `target/media-api-<version>.jar`). Between releases, `dev` carries the next version with `-SNAPSHOT`, e.g. `0.2.0-SNAPSHOT`.
- `info.version` in `openapi/media-api.yaml`, which the ui sees when it syncs the contract.

The ui repo, [ethannet-ui](https://github.com/Ethan-S-Evans/ethannet-ui), has its own version (managed with Changesets) and doesn't follow this one.

| Bump | When |
| --- | --- |
| **major** | A breaking change to the contract in `openapi/media-api.yaml` (see [API contract](features/api-contract.md#changing-the-contract)), or a large overhaul. |
| **minor** | A new endpoint or feature that doesn't break anything. |
| **patch** | A bug fix or a small internal change. |

Until the first `1.0.0` release, the version stays on `0.y.z` and breaking changes bump the minor version.

There are no changeset files. The bump is decided at release time from what merged since the last tag (`git log vX.Y.Z..dev`), so give each merge a clear message and note any contract change in it.

## Working on a feature

1. Branch from an up-to-date `dev`:

   ```
   git switch dev
   git pull
   git switch -c feature/<short-description>
   ```

2. Do the work, tests first (see [CLAUDE.md](../CLAUDE.md)).
3. Merge the branch into `dev` once `./mvnw verify` passes.

## Cutting a release

Only when the owner decides to release.

1. On a release branch off `dev`, set the release version in both places:

   ```
   git switch dev && git pull
   git switch -c feature/release-vX.Y.Z
   ./mvnw versions:set -DnewVersion=X.Y.Z -DgenerateBackupPoms=false
   # and set info.version: X.Y.Z in openapi/media-api.yaml
   ./mvnw verify
   ```

   Commit, and merge it into `dev`.

2. Merge `dev` into `main` **through a pull request**, then tag the merge commit. `main` has a GitHub ruleset (no deletion, no force-push, and CodeQL code-scanning and code-quality results required), so a direct push to `main` is refused. A pull request gets its CodeQL results before merging:

   ```
   git push origin dev
   gh pr create --base main --head dev --title "Release vX.Y.Z" --body "Release vX.Y.Z"
   # wait for the CodeQL checks, then merge with a merge commit:
   gh pr merge --merge
   git switch main && git pull
   git tag -a vX.Y.Z -m "vX.Y.Z"
   git push origin vX.Y.Z
   ```

   CodeQL runs on every push to `main` and on every pull request into it.

3. Back on `dev`, on another short branch, move to the next snapshot (`./mvnw versions:set -DnewVersion=X.Y+1.0-SNAPSHOT -DgenerateBackupPoms=false`) and merge it.
4. Tell the ui: in `ethannet-ui`, `pnpm --filter @ethannet/schema sync:api vX.Y.Z`.

## Edge cases

- **A contract change and a code-only fix in the same release**: the release takes the largest bump among them.
- **Forgot to set `info.version`**: the ui would vendor a spec claiming the old version. Fix it on `dev` and cut a patch release; don't move an existing tag.
