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

-- Stores incoming registration requests.
-- If ManualValidationStep is part of the flow the request lands here with status PENDING
-- for operator review. Automatic flows also land here, immediately set to APPROVED.
CREATE TABLE `registrations` (
    `registration_id`      uuid         NOT NULL,
    `taim_id`              uuid         NOT NULL,
    `registration_flow_id` uuid         NOT NULL,
    `entity_id`            varchar(255) NOT NULL,
    `jwks`                 TEXT,
    `metadata_policy`      TEXT,
    `trustmarks_requested` TEXT,
    `status`               varchar(20)  NOT NULL DEFAULT 'PENDING',
    `reviewed_at`          datetime,
    `reviewed_by`          varchar(255),
    `rejection_reason`     TEXT,
    `created_date`         datetime     NOT NULL,
    `last_modified_date`   datetime     NOT NULL,
    `created_by`           varchar(255),
    `last_modified_by`     varchar(255),
    PRIMARY KEY (`registration_id`),
    CONSTRAINT `fk_reg_intermediate`
        FOREIGN KEY (`taim_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`),
    CONSTRAINT `fk_reg_flow`
        FOREIGN KEY (`registration_flow_id`) REFERENCES `registration_flow` (`flow_id`)
);