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

ALTER TABLE `organization_domain`
    ADD COLUMN `instance_id` uuid NULL;

UPDATE `organization_domain` `d`
    JOIN `organization` `o` ON `o`.`organization_id` = `d`.`organization_id`
SET `d`.`instance_id` = `o`.`instance_id`;

ALTER TABLE `organization_domain`
    MODIFY COLUMN `instance_id` uuid NOT NULL;

ALTER TABLE `organization_domain`
    ADD CONSTRAINT `fk_organization_domain_instance`
        FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`);

ALTER TABLE `organization_domain`
    ADD COLUMN `held_domain` varchar(255)
        AS (CASE WHEN `status` IN ('PENDING', 'VALIDATED') THEN `domain` ELSE NULL END) PERSISTENT;

ALTER TABLE `organization_domain`
    ADD UNIQUE KEY `uk_instance_held_domain` (`instance_id`, `held_domain`);
