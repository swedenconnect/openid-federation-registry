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

-- Create flow table for registration flows linked to an organization.
CREATE TABLE `registration_flow` (
    `flow_id`            uuid     NOT NULL,
    `organization_id`    uuid     DEFAULT NULL,
    `name`               VARCHAR(255) DEFAULT NULL,
    `description`        VARCHAR(255) DEFAULT NULL,
    `flow_definition`     TEXT         DEFAULT NULL COMMENT 'JSON map representing the flow definition',
    `created_date`       DATETIME     NOT NULL,
    `last_modified_date` DATETIME     NOT NULL,
    `created_by`         VARCHAR(255) DEFAULT NULL,
    `last_modified_by`   VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`flow_id`),
    CONSTRAINT `fk_flow_organization`
        FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
);