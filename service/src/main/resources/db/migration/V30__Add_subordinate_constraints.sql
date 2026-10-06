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

-- Constraints for a subordinate statement (OpenID Federation 1.0, section 6.2), stored as JSON in the same format as
-- the claim: max_path_length, naming_constraints (permitted, excluded) and allowed_entity_types.
ALTER TABLE `subordinate`
    ADD COLUMN `constraints` TEXT DEFAULT NULL COMMENT 'Inline JSON constraints for this subordinate statement';
