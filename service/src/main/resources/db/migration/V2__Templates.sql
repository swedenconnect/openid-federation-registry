
/*
 * Copyright 2024-2025  Sweden Connect
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


-- TrustmarkIssuer

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKISSUER',
        'entity_id',
        'Entity config',
        'OPTIONS',
        '',
        'required | uuid',
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
        'trust_mark_token_validity_duration',
        'Validity for the token representing the trustmark. Expressed in hours.',
        'DURATION',
        'PT1H',
        'required | duration',
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
where (data_key = 'active'
    or data_key = 'entity_id')
  and fk_type = 'TRUSTMARKISSUER';

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTANCHOR',
        'trust_mark_issuer',
        'Trust Mark Issuer',
        'JSON',
        '',
        'json',
        'Flyway',
        'Flyway');

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
where (data_key = 'active'
    or data_key = 'entity_id')
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
where (data_key = 'active'
    or data_key = 'entity_id')
  and fk_type = 'TRUSTMARKISSUER';



INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'resolve_response_duration',
        'Duration of the response. Expressed in hours.',
        'DURATION',
        'PT1H',
        'required | duration',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'trust_anchor',
        'URL to trustanchor that will be used to build trust chain ',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'trusted_keys',
        'Trusted keys, JWKS format',
        'LARGETEXT',
        '',
        'required | jwks',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'RESOLVER',
        'step_retry_duration',
        'Time between a failed step and retry.',
        'DURATION',
        'PT1M',
        'required | duration',
        'Flyway',
        'Flyway');

-- TrustMarks

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'trustmarkissuer_id',
        'To what trustmarkissuer should this trustmark belong to',
        'OPTIONS',
        '',
        'required | uuid',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'trust_mark_entity_id',
        'Trustmark entityid, ex https://sc.swedenconnect.se/loa3',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'logo_uri',
        'Url that points to a logotype image',
        'TEXT',
        '',
        'url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'ref_uri',
        'Reference url, usually pointing to a documentation page',
        'TEXT',
        '',
        'url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARK',
        'delegation',
        'Delegation JWT according to oidf specification',
        'TEXT',
        '',
        'jwt:delegation',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'POLICIES',
        'name',
        'Name',
        'TEXT',
        '',
        'required',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'POLICIES',
        'policy',
        'Json policy document',
        'JSON',
        '',
        'required | json',
        'Flyway',
        'Flyway');

-- TrustMarkSubject
INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKSUBJECT',
        'trustmark_id',
        'TrustMark',
        'OPTIONS',
        '',
        'required',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKSUBJECT',
        'subject',
        'Subject entity id',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKSUBJECT',
        'revoked',
        'If it is revoked',
        'BOOLEAN',
        'false',
        'required',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKSUBJECT',
        'granted',
        'When it should be granted',
        'DATETIME',
        '',
        '',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'TRUSTMARKSUBJECT',
        'expires',
        'When it should be expires',
        'DATETIME',
        '',
        '',
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

-- Entity-Hosted / Entity-Connected


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'FEDERATION_ENTITY',
        'subject',
        'Subject',
        'TEXT',
        '@{entityprefix}',
        'required | url | starts_with:@{entityprefix}',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'FEDERATION_ENTITY',
        'metadata',
        'Metadata',
        'JSON',
        '',
        'json',
        'Flyway',
        'Flyway');



INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'FEDERATION_ENTITY',
        'issuer',
        'Issuer entityid',
        'TEXT',
        '@{entityprefix}',
        'required | url | starts_with:@{entityprefix}',
        'Flyway',
        'Flyway');

-- Connected Entity

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
select fk_id,
       'SUBORDINATE_ENTITY',
       data_key,
       description,
       data_type,
       data_value,
       validation,
       created_by,
       last_modified_by
from settings
where (
    data_key = 'policy_id')
  and fk_type = 'FEDERATION_ENTITY';
INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'SUBORDINATE_ENTITY',
        'subject',
        'Subject',
        'TEXT',
        '',
        'required | url',
        'Flyway',
        'Flyway');

INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'SUBORDINATE_ENTITY',
        'issuer',
        'Issuer entityid',
        'TEXT',
        '',
        'required | url ',
        'Flyway',
        'Flyway');


INSERT INTO settings(fk_id, fk_type, data_key, description, data_type, data_value, validation, created_by,
                     last_modified_by)
VALUES ('TEMPLATE',
        'SUBORDINATE_ENTITY',
        'jwks',
        'Public keys in JWKS format',
        'JSON',
        '',
        'required | jwks',
        'Flyway',
        'Flyway');



