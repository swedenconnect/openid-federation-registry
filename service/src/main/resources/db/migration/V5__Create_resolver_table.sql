/*
 * Copyright 2025 Sweden Connect
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

-- Resolver table
CREATE TABLE IF NOT EXISTS `resolver`
(
    `resolver_id`               UUID         NOT NULL DEFAULT (UUID()),
    `entity_id`                 UUID         NOT NULL,
    `active`                    BOOLEAN      NOT NULL,
    `resolve_response_duration` VARCHAR(255) NOT NULL,
    `step_cached_value_threshold` INTEGER NOT NULL,
    `trust_anchor`              VARCHAR(255) NOT NULL,
    `trusted_keys`              TEXT         NOT NULL,
    `step_retry_duration`       VARCHAR(255) NOT NULL,
    `created_by`                VARCHAR(255) NOT NULL,
    `last_modified_by`          VARCHAR(255) NOT NULL,
    `created_date`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`resolver_id`),
    FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`) ON DELETE RESTRICT
) ENGINE = InnoDB;
