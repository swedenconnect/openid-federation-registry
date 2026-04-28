/*
 * Copyright 2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.registrationflow.process;

import java.util.List;

/**
 * Minimal representation of an entity's metadata used during pipeline processing.
 *
 * @param entityId the entity identifier
 * @param scopes the scopes declared by the entity
 * @author Per Fredrik Plars
 */
public record EntityMetadata(String entityId, List<String> scopes) {
}
