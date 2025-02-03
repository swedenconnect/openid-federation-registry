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

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE', 'TRUSTMARKISSUER', 'trust-mark-token-validity-duration',
        'Validity for the token representing the trustmark. Expressed in hours.', 'NUMERIC', '1',
        'req | min:1 | max:24', 'Flyway', 'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE', 'TRUSTMARKISSUER', 'active', 'Is this module instance should be active or not', 'BOOLEAN', '', '',
        'Flyway', 'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE', 'TRUSTMARKISSUER', 'instance_id', 'To what instance will this module be added', 'OPTIONS',
        '{module_id:issuer_entity_id}', 'req', 'Flyway', 'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, data_type, data_value, validation, created_by, last_modified_by)
VALUES ('49c15858-df50-426e-ace8-99961fcfbcfd', 'TRUSTMARKISSUER', 'trust-mark-token-validity-duration', 'NUMERIC',
        '10', 'req | min:1 | max:2', 'Flyway', 'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, data_type, data_value, validation, created_by, last_modified_by)
VALUES ('49c15858-df50-426e-ace8-99961fcfbcfd', 'TRUSTMARKISSUER', 'active', 'BOOLEAN', 'TRUE', '', 'Flyway', 'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, data_type, data_value, validation, created_by, last_modified_by)
VALUES ('49c15858-df50-426e-ace8-99961fcfbcfd', 'TRUSTMARKISSUER', 'instance_id', 'OPTIONS', '', 'req', 'Flyway',
        'Flyway');

INSERT INTO module (active,
                    external_id,
                    created_by,
                    issuer_entity_id,
                    last_modified_by,
                    module_id,
                    module_type)
VALUES (1, -- active (1 = true)
        '49c15858-df50-426e-ace8-99961fcfbcfd', -- external_id
        'test_creator', -- created_by
        'issuer-entity-001', -- issuer_entity_id
        'last_modifier', -- last_modified_by
        '1', -- module_id
        'TRUSTMARKISSUER' -- module_type
       );

INSERT INTO instance (instance_id,
                      name)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        'SwedenConnect');

INSERT INTO instance (instance_id,
                      name)
VALUES ('c9a646e8-89f2-4d3e-bdaf-7e9b5f424123',
        'ENA');