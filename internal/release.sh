#!/usr/bin/env bash
#
# Copyright 2026 Sweden Connect
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
#  limitations under the License.
#

#
# Prepares and tags a release: works out the next version from existing git tags,
# lets you confirm or override it, creates a release_<version> branch, bumps
# the project version in all pom.xml files, builds the project,
# commits, tags the release commit, bumps to the next development version on
# the same branch, and pushes. The branch is then merged into main with a merge
# commit (not squash/rebase) so that the tag stays reachable from main.
#
# Every command is printed before it runs. If a command fails you are asked
# whether to run it again.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

REMOTE="origin"

# run <command...>: prints the command, runs it, and on failure asks whether to run it again.
run() {
  while true; do
    echo "\$ $*"
    if "$@"; then
      return 0
    fi
    echo "Command failed: $*" >&2
    read -r -p "Run it again? [y/N]: " RETRY
    case "$RETRY" in
      y|Y|yes|Yes|YES) ;;
      *) echo "Aborting." >&2; exit 1 ;;
    esac
  done
}

echo "== Release branch preparation =="

# Untracked files are allowed; only staged or modified tracked files block the release.
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
  echo "Working tree has uncommitted changes. Commit or stash them before releasing." >&2
  git status --short --untracked-files=no >&2
  exit 1
fi

CURRENT_BRANCH="$(git branch --show-current)"
if [ "$CURRENT_BRANCH" != "main" ]; then
  echo "This script must be run from 'main' (currently on '$CURRENT_BRANCH')." >&2
  exit 1
fi

echo "Pulling latest changes on main ..."
run git pull --ff-only "$REMOTE" main

echo "Fetching tags from $REMOTE ..."
run git fetch --tags --quiet "$REMOTE"

LATEST_TAG="$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' | sort -V | tail -1)"

if [ -z "$LATEST_TAG" ]; then
  echo "No tags found in the vX.Y.Z format."
  read -r -p "Enter starting version (X.Y.Z): " SUGGESTED_VERSION
else
  VERSION_NO_V="${LATEST_TAG#v}"
  MAJOR="$(echo "$VERSION_NO_V" | cut -d. -f1)"
  MINOR="$(echo "$VERSION_NO_V" | cut -d. -f2)"
  PATCH="$(echo "$VERSION_NO_V" | cut -d. -f3)"
  SUGGESTED_VERSION="${MAJOR}.${MINOR}.$((PATCH + 1))"
  echo "Latest tag: $LATEST_TAG"
fi

echo "Suggested next version: $SUGGESTED_VERSION"
read -r -p "Use this version? [Y/n/enter your own version]: " ANSWER

case "$ANSWER" in
  ""|y|Y|yes|Yes|YES)
    VERSION="$SUGGESTED_VERSION"
    ;;
  n|N|no|No|NO)
    read -r -p "Enter desired version (X.Y.Z): " VERSION
    ;;
  *)
    VERSION="$ANSWER"
    ;;
esac

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Invalid version format: '$VERSION' (expected X.Y.Z)" >&2
  exit 1
fi

V_MAJOR="$(echo "$VERSION" | cut -d. -f1)"
V_MINOR="$(echo "$VERSION" | cut -d. -f2)"
V_PATCH="$(echo "$VERSION" | cut -d. -f3)"
NEXT_DEV_VERSION="${V_MAJOR}.${V_MINOR}.$((V_PATCH + 1))-SNAPSHOT"

BRANCH="release_${VERSION//./_}"

if git show-ref --verify --quiet "refs/heads/$BRANCH" || git ls-remote --exit-code --heads "$REMOTE" "$BRANCH" >/dev/null 2>&1; then
  echo "Branch '$BRANCH' already exists locally or on $REMOTE." >&2
  exit 1
fi

echo "Creating branch '$BRANCH' ..."
run git checkout -b "$BRANCH"

echo "Setting the version to $VERSION in all pom.xml files ..."
run ./internal/set-version.sh "$VERSION"

echo "Building the project ..."
run mvn clean install

echo
echo "== Reminder =="
echo "Remember to update docs/release-notes.md with the release notes for version $VERSION before continuing."
read -r -p "Press Enter once the release notes are updated (or Ctrl+C to abort here) ..." _

run git add -- '**/pom.xml' pom.xml docs/release-notes.md
run git commit -m "choir: Prepare release $VERSION"

echo
echo "The release commit must be on $REMOTE before it can be tagged."
read -r -p "Push branch '$BRANCH' to $REMOTE? [y/N]: " PUSH_ANSWER
case "$PUSH_ANSWER" in
  y|Y|yes|Yes|YES)
    run git push -u "$REMOTE" "$BRANCH"
    ;;
  *)
    echo "Push skipped. Aborting before tagging; '$BRANCH' exists locally only." >&2
    exit 1
    ;;
esac

echo
echo "== Tagging =="
if git show-ref --verify --quiet "refs/tags/v$VERSION" || git ls-remote --exit-code --tags "$REMOTE" "v$VERSION" >/dev/null 2>&1; then
  echo "Tag 'v$VERSION' already exists locally or on $REMOTE." >&2
  exit 1
fi

echo "Tagging the release commit on '$BRANCH' as v$VERSION."
echo "Pushing the tag triggers the Docker release workflow."
read -r -p "Tag and push v$VERSION to $REMOTE? [y/N]: " TAG_ANSWER
case "$TAG_ANSWER" in
  y|Y|yes|Yes|YES)
    run git tag "v$VERSION"
    run git push "$REMOTE" "v$VERSION"
    ;;
  *)
    echo "Tagging skipped. Aborting before the development version is set." >&2
    exit 1
    ;;
esac

echo
echo "== Next development version =="
echo "Setting the version to $NEXT_DEV_VERSION in all pom.xml files ..."
run ./internal/set-version.sh "$NEXT_DEV_VERSION"
run git add -- '**/pom.xml' pom.xml
run git commit -m "choir: new version $NEXT_DEV_VERSION"

echo
echo "Pushing the development version commit on '$BRANCH' ..."
run git push "$REMOTE" "$BRANCH"

echo
echo "== Pull request =="
echo "Open a pull request from '$BRANCH' into main (e.g. via the link printed by the push above)."

echo
echo "Done. Tag v$VERSION is pushed and '$BRANCH' is on $NEXT_DEV_VERSION."
echo "Merge the pull request with a MERGE COMMIT (not squash/rebase); main then ends up on $NEXT_DEV_VERSION."
