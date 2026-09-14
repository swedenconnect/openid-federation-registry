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
 * limitations under the License.
 */

ALTER TABLE `organization`
    ADD COLUMN `legal_name` varchar(255) NULL;

CREATE TABLE `organization_domain`
(
    `domain_id`          uuid         NOT NULL,
    `organization_id`    uuid         NOT NULL,
    `domain`             varchar(255) NOT NULL,
    `status`             varchar(20)  NOT NULL,
    `rejection_reason`   TEXT         NULL,
    `reviewed_at`        datetime     NULL,
    `reviewed_by`        varchar(255) NULL,
    `created_date`       datetime     NOT NULL,
    `last_modified_date` datetime     NOT NULL,
    `created_by`         varchar(255) DEFAULT NULL,
    `last_modified_by`   varchar(255) DEFAULT NULL,
    PRIMARY KEY (`domain_id`),
    UNIQUE KEY `uk_organization_domain` (`organization_id`, `domain`),
    CONSTRAINT `fk_organization_domain_organization`
        FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
);

CREATE TABLE `organization_trust_mark`
(
    `organization_id`    uuid         NOT NULL,
    `trust_mark_type`    varchar(255) NOT NULL,
    `created_date`       datetime     NOT NULL,
    `last_modified_date` datetime     NOT NULL,
    `created_by`         varchar(255) DEFAULT NULL,
    `last_modified_by`   varchar(255) DEFAULT NULL,
    PRIMARY KEY (`organization_id`, `trust_mark_type`),
    CONSTRAINT `fk_organization_trust_mark_organization`
        FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
);
