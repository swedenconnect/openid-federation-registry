# Release Instructions

How to cut a release of OpenID Federation Registry.

## Prerequisites

- Clean working tree, checked out on `main` with the latest changes pulled.
- Push access to `origin`.
- Maven installed and able to build the project locally.

## Run the release script

Run the release script from the repository root:

```
./internal/release.sh
```

The script will:

1. Verify the working tree is clean.
2. Verify you're on `main` (fails if not).
3. Pull the latest changes on `main`.
4. Fetch tags from `origin` and suggest the next version (latest `vX.Y.Z` tag, patch bumped by one).
5. Ask you to confirm the suggested version or enter a different one.
6. Create a `release_X_Y_Z` branch (underscores, e.g. `release_0_11_15`).
7. Set the new version in every `pom.xml` with `./internal/set-version.sh` (`mvn versions:set` plus the `guitest` parent reference).
8. Run `mvn clean install` to build and test the release version.
9. Pause and remind you to update [`docs/release-notes.md`](../docs/release-notes.md) with the changes in this release —
   do this now, before continuing.
10. Commit the version bump and release notes as `choir: Prepare release X.Y.Z`.
11. Ask whether to push the branch (the release commit must be on `origin` before it can be tagged), then whether to tag that commit `vX.Y.Z` and push the tag. Pushing the tag triggers the Docker release workflow
    (`.github/workflows/release.yml` → `docker-release.yml`).
12. Set the version in every `pom.xml` to the next patch version with a `-SNAPSHOT` suffix and commit it on the
    same branch as `choir: new version X.Y.Z-SNAPSHOT`.
13. Push the branch again with the development version commit. Open the pull request into `main` yourself (the push prints a link).

Every command is printed before it runs. If a command fails, the script asks whether to run it again; answering no
aborts the script.

**Merge the pull request with a merge commit** (not squash or rebase). The tag points at the release commit on the
branch, and only a merge commit keeps that commit reachable from `main`. `main` then ends up on the next
`-SNAPSHOT` version.

That's the whole release — nothing to run manually afterward, apart from merging the pull request.

## Version scheme

- Tags are `vX.Y.Z` (e.g. `v0.11.14`).
- The project version in every `pom.xml` always matches the tag without
  the `v` prefix, with a `-SNAPSHOT` suffix while in development.
- Releases are patch bumps unless a change explicitly warrants a minor/major bump — if so, just answer the version
  prompt in `release.sh` with the version you want instead of accepting the suggestion.

## Troubleshooting

- **"Working tree has uncommitted changes"** — the script refuses to start with a dirty working tree. Commit,
  stash, or clean up first.
- **"This script must be run from 'main'"** — switch to `main` (`git checkout main`) before running the script.
- **"Branch ... already exists"** — a `release_X_Y_Z` branch already exists locally or on `origin`. Delete it or pick a
  different version.
- **"Tag ... already exists"** — `vX.Y.Z` is already tagged locally or on `origin`. This shouldn't happen unless a
  release was already cut for that version, or a previous run of the script got interrupted after tagging.
- If `mvn clean install` fails during step 8, fix the issue on the release branch, commit, and re-run
  `./internal/set-version.sh <version>` / `mvn clean install` manually — no need to restart the whole script.
- If the script is interrupted after the tag was pushed, do not run it again. Finish by hand on the release branch:
  set the next `-SNAPSHOT` version with `./internal/set-version.sh`, commit, push the branch, and open the pull request.
