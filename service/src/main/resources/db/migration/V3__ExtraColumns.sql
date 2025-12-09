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

-- Add columns for EntityEntity
ALTER TABLE `entities`
    ADD COLUMN `metadata` JSON COMMENT 'Metadata for FederationEntity and HostedEntity',
    ADD COLUMN `jwks` TEXT
COMMENT
'JWKSet for SubordinateEntity',
    ADD COLUMN `subject` VARCHAR(255) COMMENT
'Subject',
    ADD COLUMN `issuer` VARCHAR(255) COMMENT
'Issuer';

-- Add columns for TrustMarkEntity
ALTER TABLE `trustmark`
    ADD COLUMN `trustmarkissuer_id` VARCHAR(255) COMMENT 'Trustmark issuer ID',
    ADD COLUMN `trustmark_entity_id` VARCHAR(255)
COMMENT
'Trustmark entity ID',
    ADD COLUMN `logo_uri` VARCHAR(512) COMMENT
'URL to logotype image',
    ADD COLUMN `ref_uri` VARCHAR(512) COMMENT
'Reference URL',
    ADD COLUMN `delegation` TEXT COMMENT
'Delegation JWT according to OIDF specification';

-- Add columns for TrustMarkSubjectEntity
ALTER TABLE `trustmark_subject`
    ADD COLUMN `trustmark_id_ref` VARCHAR(255) COMMENT 'Reference to trustmark ID',
    ADD COLUMN `subject` VARCHAR(255)
COMMENT
'Subject entity ID',
    ADD COLUMN `revoked` BOOLEAN DEFAULT FALSE COMMENT
'If the trustmark is revoked',
    ADD COLUMN `granted` DATETIME COMMENT
'When the trustmark was granted',
    ADD COLUMN `expires` DATETIME COMMENT
'When the trustmark expires';

-- Note: PolicyEntity already has a policy column (TEXT), but we keep it as is
-- since it stores JSON policy documents which are complex nested structures

ALTER TABLE `module`
    ADD COLUMN `active` BOOLEAN DEFAULT TRUE
        COMMENT
            'If module is active',
    ADD COLUMN `resolve_response_duration` VARCHAR(255)
COMMENT
'Response duration for Resolver',
    ADD COLUMN `trust_anchor` VARCHAR(255) COMMENT
'Trust anchor entity ID for Resolver',
    ADD COLUMN `trusted_keys` TEXT COMMENT
'Trusted keys (JWKS) for Resolver',
    ADD COLUMN `step_retry_duration` VARCHAR(255) COMMENT
'Step retry duration for Resolver',
    ADD COLUMN `trust_mark_issuers` JSON COMMENT
'Trust mark issuers for TrustAnchor',
    ADD COLUMN `trust_mark_token_validity_duration` VARCHAR(255) COMMENT
'Trust mark token validity duration for TrustmarkIssuer';

ALTER TABLE `policies`
    ADD COLUMN name VARCHAR(255),
    ADD COLUMN policy TEXT AFTER name;
