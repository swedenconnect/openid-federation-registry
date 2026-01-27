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

CREATE TABLE IF NOT EXISTS `subordinate`
(
    `subordinate_id`        UUID         NOT NULL DEFAULT (UUID()),
    `ta_im_id`              UUID         NOT NULL,
    `policy_id`             UUID                  DEFAULT NULL,
    `jwks`                  TEXT                  DEFAULT NULL COMMENT 'JWKSet for SubordinateEntity',
    `entityidentifier` VARCHAR(255) DEFAULT NULL COMMENT 'entityidentifier',
    `crit`                  TEXT                  DEFAULT NULL COMMENT 'List of crit claims',
    `metadata_policy_crit`  TEXT                  DEFAULT NULL COMMENT 'List of metadata_policy_crit',
    `ec_location`           VARCHAR(255)          DEFAULT NULL COMMENT 'Location where the actual entity statement is placed',
    `ec_location_automatic` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'When true, eclocation will be loaded from the hosted entity with the same issuer entityid',
    `created_date`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`            VARCHAR(255) NOT NULL,
    `last_modified_by`      VARCHAR(255) NOT NULL,
    PRIMARY KEY (`subordinate_id`),
    UNIQUE KEY `uk_subordinate_id_entityidentifier` (`subordinate_id`, `entityidentifier`),
    FOREIGN KEY (`ta_im_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`) ON DELETE RESTRICT,
    FOREIGN KEY (`policy_id`) REFERENCES `policies` (`policy_id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

