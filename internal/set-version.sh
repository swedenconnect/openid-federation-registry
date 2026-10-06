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
# Usage: internal/set-version.sh <version>
#
# Sets the project version in the parent pom and in every module that refers to it.
# guitest is not a module of the parent pom, so its parent reference is updated separately.
set -euo pipefail

VERSION="${1:?Usage: $0 <version>}"

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-SNAPSHOT)?$ ]]; then
  echo "Invalid version: '$VERSION' (expected X.Y.Z or X.Y.Z-SNAPSHOT)" >&2
  exit 1
fi

cd "$(git rev-parse --show-toplevel)"

mvn --no-transfer-progress -q versions:set -DnewVersion="$VERSION" -DgenerateBackupPoms=false

VERSION="$VERSION" perl -0pi -e \
  's{(<artifactId>oidf-entity-registry-parent</artifactId>\s*<version>)[^<]+(</version>)}{$1$ENV{VERSION}$2}' \
  guitest/pom.xml
