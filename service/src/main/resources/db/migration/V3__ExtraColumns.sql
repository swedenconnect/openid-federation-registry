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

ALTER TABLE `policies`
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN policy TEXT AFTER name;

ALTER TABLE `entities`
    ADD COLUMN jsondata TEXT;

ALTER TABLE `module`
    ADD COLUMN jsondata TEXT;

ALTER TABLE `trustmark`
    ADD COLUMN jsondata TEXT;

ALTER TABLE `trustmark_subject`
    ADD COLUMN jsondata TEXT;

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'FEDERATION_ENTITY',
        'policy_id',
        'Policy',
        'OPTIONS',
        '',
        '',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'SUBORDINATE_ENTITY',
        'policy_id',
        'Policy',
        'OPTIONS',
        '',
        '',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'HOSTED_ENTITY',
        'policy_id',
        'Policy',
        'OPTIONS',
        '',
        '',
        'Flyway',
        'Flyway');