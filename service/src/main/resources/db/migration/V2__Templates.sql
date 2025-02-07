-- TrustmarkIssuer
INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKISSUER',
        'entity_identifier',
        'The EntityId that this module will have',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKISSUER',
        'alias',
        'Alias where the module is mounted ex https://www.swedenconnect.se/{alias}',
        'TEXT',
        '',
        'required',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKISSUER',
        'active',
        'If this module instance should be active or not',
        'BOOLEAN',
        'true',
        'required',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKISSUER',
        'instance_id',
        'Instance that this TMS will belong to',
        'OPTIONS',
        '',
        'required',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKISSUER',
        'trust-mark-token-validity-duration',
        'Validity for the token representing the trustmark. Expressed in hours.',
        'NUMERIC',
        '1',
        'required | min:1 | max:24',
        'Flyway',
        'Flyway');

-- TrustAnchor
INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
select fk_id,
       'TRUSTANCHOR',
       data_key,
       description,
       data_type,
       data_value,
       validation,
       created_by,
       last_modified_by
from settings
where (data_key = 'instance_id'
    or data_key = 'active'
    or data_key = 'entity_identifier'
    or data_key = 'alias')
  and fk_type = 'TRUSTMARKISSUER';

-- INTERMEDIATE
INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
select fk_id,
       'INTERMEDIATE',
       data_key,
       description,
       data_type,
       data_value,
       validation,
       created_by,
       last_modified_by
from settings
where (data_key = 'instance_id'
    or data_key = 'active'
    or data_key = 'entity_identifier'
    or data_key = 'alias')
  and fk_type = 'TRUSTMARKISSUER';

-- Resolver
INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
select fk_id,
       'RESOLVER',
       data_key,
       description,
       data_type,
       data_value,
       validation,
       created_by,
       last_modified_by
from settings
where (data_key = 'instance_id'
    or data_key = 'active'
    or data_key = 'entity_identifier'
    or data_key = 'alias')
  and fk_type = 'TRUSTMARKISSUER';



INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'resolve-response-duration',
        'Duration of the response. Expressed in hours.',
        'NUMERIC',
        '2',
        'required | min:1 | max:48',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'trust-anchor',
        'URL to trustanchor that will be used to build trustchanin ',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'trusted-keys',
        'Trusted keys, JWKS format',
        'TEXT',
        '',
        'required | jwks',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'step-retry-time',
        'Time between a failed step and retry. Expressed in sec',
        'NUMERIC',
        '20',
        'required | min:30 | max:180',
        'Flyway',
        'Flyway');


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


-- TrustMarks

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'trust-mark-id',
        'Trustmark identity,',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'logo-uri',
        'Url that points to a logotype image',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'ref-uri',
        'Reference url, usually pointing to a documentation page',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'delegation',
        'Delegation JWK according to oidf specification',
        'TEXT',
        '',
        'required | jwk:delegation',
        'Flyway',
        'Flyway');






