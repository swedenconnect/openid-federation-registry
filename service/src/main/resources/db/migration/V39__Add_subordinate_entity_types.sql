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

-- The Entity Type Identifiers of a subordinate and whether it is an Intermediate Entity. The federation services use
-- them for the entity_type and intermediate filters of the subordinate listing. The entity types are stored as a JSON
-- array of strings, in the same way as crit. Existing subordinates have no entity types until they are set.
ALTER TABLE `subordinate`
    ADD COLUMN `entity_types` TEXT DEFAULT NULL
        COMMENT 'Entity Type Identifiers of the subordinate, a list of strings',
    ADD COLUMN `is_intermediate` boolean NOT NULL DEFAULT FALSE
        COMMENT 'True if the subordinate is an Intermediate Entity';
