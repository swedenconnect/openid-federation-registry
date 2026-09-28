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
# Prepares a release branch: works out the next version from existing git tags,
# lets you confirm or override it, creates a release_<version> branch, bumps
# the service-revision property in all pom.xml files, builds the project,
# commits, and pushes the branch to origin.
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

REMOTE="origin"

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
git pull "$REMOTE" main

echo "Fetching tags from $REMOTE ..."
git fetch --tags --quiet "$REMOTE" || echo "Warning: could not fetch tags from $REMOTE, using local tags."

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

echo "Creating branch '$BRANCH' from '$(git branch --show-current)' ..."
git checkout -b "$BRANCH"

echo "Setting service-revision to $VERSION in all pom.xml files ..."
mvn versions:set-property -Dproperty=service-revision -DnewVersion="$VERSION" -DgenerateBackupPoms=false

echo "Building the project ..."
mvn clean install

echo
echo "== Reminder =="
echo "Remember to update docs/release-notes.md with the release notes for version $VERSION before continuing."
read -r -p "Press Enter once the release notes are updated (or Ctrl+C to abort here) ..." _

git add -- '**/pom.xml' pom.xml docs/release-notes.md
git commit -m "choir: Prepare release $VERSION"

echo
read -r -p "Push branch '$BRANCH' to $REMOTE? [y/N]: " PUSH_ANSWER
case "$PUSH_ANSWER" in
  y|Y|yes|Yes|YES)
    git push -u "$REMOTE" "$BRANCH"
    echo "Branch '$BRANCH' has been pushed to $REMOTE."
    ;;
  *)
    echo "Push skipped. Branch '$BRANCH' exists locally but has not been pushed."
    ;;
esac

echo
echo "== Tagging =="
echo "Open a pull request from '$BRANCH' into main, get it reviewed, and merge it."
read -r -p "Press Enter once '$BRANCH' has been merged into main (or Ctrl+C to abort here) ..." _

if git show-ref --verify --quiet "refs/tags/v$VERSION" || git ls-remote --exit-code --tags "$REMOTE" "v$VERSION" >/dev/null 2>&1; then
  echo "Tag 'v$VERSION' already exists locally or on $REMOTE." >&2
  exit 1
fi

echo "Checking out main and pulling latest ..."
git checkout main
git pull "$REMOTE" main

echo "Tagging v$VERSION and pushing the tag ..."
git tag "v$VERSION"
git push "$REMOTE" "v$VERSION"

echo
echo "== Next development version =="
echo "Setting service-revision to $NEXT_DEV_VERSION in all pom.xml files ..."
mvn versions:set-property -Dproperty=service-revision -DnewVersion="$NEXT_DEV_VERSION" -DgenerateBackupPoms=false

git add -- '**/pom.xml' pom.xml
git commit -m "choir: new version"
git push "$REMOTE" main

echo
echo "Done. Released version: $VERSION (tag v$VERSION), main is now on $NEXT_DEV_VERSION."
