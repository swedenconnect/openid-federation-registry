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

INSERT INTO instance (instance_id,
                      name,
                      created_by,
                      last_modified_by)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        'SwedenConnect',
        'Flyway',
        'Flyway');

INSERT INTO instance (instance_id,
                      name,
                      created_by,
                      last_modified_by)
VALUES ('c9a646e8-89f2-4d3e-bdaf-7e9b5f424123',
        'ENA',
        'Flyway',
        'Flyway');

INSERT INTO module (external_id,
                    instance_id,
                    userdomain_id,
                    created_by,
                    last_modified_by,
                    module_id,
                    module_type)
VALUES ('49c15858-df50-426e-ace8-99961fcfbcfd',
        '550e8400-e29b-41d4-a716-446655440000',
        3000,
        'Flyway',
        'Flyway',
        '1000',
        'TRUSTMARKISSUER');

INSERT INTO module (external_id,
                    instance_id,
                    userdomain_id,
                    created_by,
                    last_modified_by,
                    module_id,
                    module_type)
VALUES ('49c15858-df50-426e-ace8-99961fcfbcfq',
        'c9a646e8-89f2-4d3e-bdaf-7e9b5f424123',
        3000,
        'Flyway',
        'Flyway',
        '2000',
        'TRUSTMARKISSUER');


INSERT INTO settings(fk_id,
                     fk_type,
                     data_key,
                     data_type,
                     data_value,
                     validation,
                     created_by,
                     last_modified_by)

VALUES ('1000',
        'TRUSTMARKISSUER',
        'trust-mark-token-validity-duration',
        'NUMERIC',
        '10',
        'req | min:1 | max:2',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id,
                     fk_type,
                     data_key,
                     data_type,
                     data_value,
                     validation,
                     created_by,
                     last_modified_by)
VALUES ('1000',
        'TRUSTMARKISSUER',
        'active',
        'BOOLEAN',
        'TRUE',
        '',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id,
                     fk_type,
                     data_key,
                     data_type,
                     data_value,
                     validation,
                     created_by,
                     last_modified_by)
VALUES ('1000',
        'TRUSTMARKISSUER',
        'instance_id',
        'OPTIONS',
        '',
        'req',
        'Flyway',
        'Flyway');


