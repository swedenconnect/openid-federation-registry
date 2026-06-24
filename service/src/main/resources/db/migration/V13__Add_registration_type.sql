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
 *  limitations under the License.
 */

ALTER TABLE `registrations`
    ADD COLUMN `registration_type` VARCHAR(30) NOT NULL DEFAULT 'SUBORDINATE';

-- Join table linking trust mark issuers to registration flows.
CREATE TABLE `tm_issuer_flow_assignment` (
    `assign_id`            uuid         NOT NULL,
    `trustmark_issuer_id`  uuid         NOT NULL,
    `flow_id`              uuid         NOT NULL,
    `created_date`         DATETIME     NOT NULL,
    `last_modified_date`   DATETIME     NOT NULL,
    `created_by`           VARCHAR(255) DEFAULT NULL,
    `last_modified_by`     VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`assign_id`),
    UNIQUE KEY `uq_tmifa_issuer_flow` (`trustmark_issuer_id`, `flow_id`),
    CONSTRAINT `fk_tmifa_issuer`
        FOREIGN KEY (`trustmark_issuer_id`) REFERENCES `trustmark_issuer` (`trustmark_issuer_id`),
    CONSTRAINT `fk_tmifa_flow`
        FOREIGN KEY (`flow_id`) REFERENCES `registration_flow` (`flow_id`)
);
