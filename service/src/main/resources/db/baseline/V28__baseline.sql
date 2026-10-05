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

-- Baseline of the full schema for fresh installations. Equivalent to running legacy/V1 - legacy/V28.
-- Databases that already have a Flyway history use db/legacy instead, see FlywayLocationsConfiguration.

SET FOREIGN_KEY_CHECKS = 0;


CREATE TABLE `entities` (
  `entity_id` uuid NOT NULL DEFAULT uuid(),
  `organization_id` uuid NOT NULL,
  `entity_type` varchar(20) NOT NULL,
  `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Metadata for FederationEntity and HostedEntity' CHECK (json_valid(`metadata`)),
  `subject` varchar(255) DEFAULT NULL COMMENT 'Subject',
  `issuer` varchar(255) DEFAULT NULL COMMENT 'Issuer',
  `crit` text DEFAULT NULL COMMENT 'List of crit claims',
  `metadata_policy_crit` text DEFAULT NULL COMMENT 'List of metadata_policy_crit',
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `ec_location` varchar(255) DEFAULT NULL COMMENT 'Location where the actual entity statement is placed',
  `authorityhints` text DEFAULT NULL COMMENT 'Authority hints',
  `ec_location_automatic` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'When true, eclocation will be loaded from the hosted entity with the same issuer entityid',
  `trustmarksources` text DEFAULT NULL COMMENT 'Trustmark sources that can be used to include trustmarks',
  `signing_key_ids` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`entity_id`),
  UNIQUE KEY `uk_issuer` (`issuer`),
  KEY `organization_id` (`organization_id`),
  CONSTRAINT `fk_entities_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `instance` (
  `instance_id` uuid NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `organization` (
  `organization_id` uuid NOT NULL DEFAULT uuid(),
  `instance_id` uuid NOT NULL,
  `org_number` varchar(255) NOT NULL COMMENT 'Org id that matches the claim in JWT token.',
  `org_name` varchar(255) DEFAULT NULL COMMENT 'Org name that matches the claim in JWT token.',
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `legal_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`organization_id`),
  UNIQUE KEY `uk_instance_org_number` (`instance_id`,`org_number`),
  KEY `instance_id` (`instance_id`),
  CONSTRAINT `fk_organization_instance` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `organization_domain` (
  `domain_id` uuid NOT NULL,
  `organization_id` uuid NOT NULL,
  `domain` varchar(255) NOT NULL,
  `status` varchar(20) NOT NULL,
  `rejection_reason` text DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `reviewed_by` varchar(255) DEFAULT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `instance_id` uuid NOT NULL,
  `held_domain` varchar(255) GENERATED ALWAYS AS (case when `status` in ('PENDING','VALIDATED') then `domain` else NULL end) STORED,
  PRIMARY KEY (`domain_id`),
  UNIQUE KEY `uk_organization_domain` (`organization_id`,`domain`),
  UNIQUE KEY `uk_instance_held_domain` (`instance_id`,`held_domain`),
  CONSTRAINT `fk_organization_domain_instance` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`),
  CONSTRAINT `fk_organization_domain_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `organization_trust_mark` (
  `organization_id` uuid NOT NULL,
  `trust_mark_type` varchar(255) NOT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`organization_id`,`trust_mark_type`),
  CONSTRAINT `fk_organization_trust_mark_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `registration_flow` (
  `flow_id` uuid NOT NULL,
  `organization_id` uuid DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `flow_definition` text DEFAULT NULL COMMENT 'JSON map representing the flow definition',
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `description_sv` text DEFAULT NULL,
  `technology` varchar(10) DEFAULT NULL,
  `entity_type` varchar(100) DEFAULT NULL,
  `flow_type` varchar(30) NOT NULL DEFAULT 'INTERMEDIATE',
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`flow_id`),
  KEY `fk_flow_organization` (`organization_id`),
  CONSTRAINT `fk_flow_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `registration_flow_assignment` (
  `assign_id` uuid NOT NULL,
  `ta_im_id` uuid NOT NULL,
  `flow_id` uuid NOT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`assign_id`),
  UNIQUE KEY `uq_rfa_intermediate_flow` (`ta_im_id`,`flow_id`),
  KEY `fk_rfa_flow` (`flow_id`),
  CONSTRAINT `fk_rfa_flow` FOREIGN KEY (`flow_id`) REFERENCES `registration_flow` (`flow_id`),
  CONSTRAINT `fk_rfa_intermediate` FOREIGN KEY (`ta_im_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `registrations` (
  `registration_id` uuid NOT NULL,
  `assign_id` uuid NOT NULL,
  `entity_id` varchar(255) NOT NULL,
  `jwks` text DEFAULT NULL,
  `metadata_policy` text DEFAULT NULL,
  `trustmarks_requested` text DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `reviewed_at` datetime DEFAULT NULL,
  `reviewed_by` varchar(255) DEFAULT NULL,
  `rejection_reason` text DEFAULT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  `organization_id` uuid DEFAULT NULL,
  `registration_type` varchar(30) NOT NULL DEFAULT 'SUBORDINATE',
  `step_results` text DEFAULT NULL,
  `pending_step_index` int(11) DEFAULT NULL,
  `parent_registration_id` uuid DEFAULT NULL,
  `request_metadata` text DEFAULT NULL,
  PRIMARY KEY (`registration_id`),
  KEY `fk_reg_assignment` (`assign_id`),
  KEY `fk_reg_organization` (`organization_id`),
  KEY `fk_registration_parent` (`parent_registration_id`),
  CONSTRAINT `fk_reg_assignment` FOREIGN KEY (`assign_id`) REFERENCES `registration_flow_assignment` (`assign_id`),
  CONSTRAINT `fk_reg_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`),
  CONSTRAINT `fk_registration_parent` FOREIGN KEY (`parent_registration_id`) REFERENCES `registrations` (`registration_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `resolver` (
  `resolver_id` uuid NOT NULL DEFAULT uuid(),
  `entity_id` uuid NOT NULL,
  `active` tinyint(1) NOT NULL,
  `resolve_response_duration` varchar(255) NOT NULL,
  `step_cached_value_threshold` int(11) NOT NULL,
  `trust_anchor` varchar(255) NOT NULL,
  `trusted_keys` text NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`resolver_id`),
  KEY `entity_id` (`entity_id`),
  CONSTRAINT `fk_resolver_entity` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `subordinate` (
  `subordinate_id` uuid NOT NULL DEFAULT uuid(),
  `ta_im_id` uuid NOT NULL,
  `jwks` text DEFAULT NULL COMMENT 'JWKSet for SubordinateEntity',
  `entityidentifier` varchar(255) DEFAULT NULL COMMENT 'entityidentifier',
  `crit` text DEFAULT NULL COMMENT 'List of crit claims',
  `metadata_policy_crit` text DEFAULT NULL COMMENT 'List of metadata_policy_crit',
  `ec_location` varchar(255) DEFAULT NULL COMMENT 'Location where the actual entity statement is placed',
  `ec_location_automatic` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'When true, eclocation will be loaded from the hosted entity with the same issuer entityid',
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `metadata_policy` text DEFAULT NULL COMMENT 'Inline JSON metadata policy for this subordinate statement',
  `metadata` text DEFAULT NULL COMMENT 'Inline JSON metadata for this subordinate statement',
  PRIMARY KEY (`subordinate_id`),
  UNIQUE KEY `uq_subordinate_taim_entityidentifier` (`ta_im_id`,`entityidentifier`),
  CONSTRAINT `fk_subordinate_trustanchor_intermediate` FOREIGN KEY (`ta_im_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `tm_flow_assignment` (
  `assign_id` uuid NOT NULL,
  `trustmark_id` uuid NOT NULL,
  `flow_id` uuid NOT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`assign_id`),
  UNIQUE KEY `uq_tmfa_trustmark_flow` (`trustmark_id`,`flow_id`),
  KEY `fk_tmfa_flow` (`flow_id`),
  CONSTRAINT `fk_tmfa_flow` FOREIGN KEY (`flow_id`) REFERENCES `registration_flow` (`flow_id`),
  CONSTRAINT `fk_tmfa_trustmark` FOREIGN KEY (`trustmark_id`) REFERENCES `trustmark` (`trustmark_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `tm_issuer_flow_assignment` (
  `assign_id` uuid NOT NULL,
  `trustmark_issuer_id` uuid NOT NULL,
  `flow_id` uuid NOT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`assign_id`),
  UNIQUE KEY `uq_tmifa_issuer_flow` (`trustmark_issuer_id`,`flow_id`),
  KEY `fk_tmifa_flow` (`flow_id`),
  CONSTRAINT `fk_tmifa_flow` FOREIGN KEY (`flow_id`) REFERENCES `registration_flow` (`flow_id`),
  CONSTRAINT `fk_tmifa_issuer` FOREIGN KEY (`trustmark_issuer_id`) REFERENCES `trustmark_issuer` (`trustmark_issuer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `trustanchor_intermediate` (
  `ta_im_id` uuid NOT NULL DEFAULT uuid(),
  `organization_id` uuid NOT NULL,
  `entity_id` uuid NOT NULL,
  `module_type` varchar(20) DEFAULT NULL,
  `active` tinyint(1) DEFAULT 1 COMMENT 'If module is active',
  `trust_mark_issuers` varchar(255) DEFAULT NULL COMMENT 'List of Trust mark issuers for TrustAnchor',
  `trust_mark_token_validity_duration` varchar(255) DEFAULT NULL COMMENT 'Trust mark token validity duration for TrustmarkIssuer',
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  PRIMARY KEY (`ta_im_id`),
  UNIQUE KEY `uk_ta_im_id_module_type` (`entity_id`,`module_type`),
  KEY `organization_id` (`organization_id`),
  KEY `entity_id` (`entity_id`),
  CONSTRAINT `fk_ta_im_entity` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`),
  CONSTRAINT `fk_ta_im_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `trustmark` (
  `trustmark_id` uuid NOT NULL DEFAULT uuid(),
  `trustmarkissuer_id` uuid NOT NULL,
  `trustmark_type` varchar(255) DEFAULT NULL COMMENT 'Trustmark type',
  `logo_uri` varchar(512) DEFAULT NULL COMMENT 'URL to logotype image',
  `ref_uri` varchar(512) DEFAULT NULL COMMENT 'Reference URL',
  `delegation` text DEFAULT NULL COMMENT 'Delegation JWT according to OIDF specification',
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  PRIMARY KEY (`trustmark_id`),
  UNIQUE KEY `uk_trustmarkissuer_id_trustmark_entity_id` (`trustmarkissuer_id`,`trustmark_type`),
  KEY `fk_trustmark_trustmark_issuer` (`trustmarkissuer_id`),
  CONSTRAINT `fk_trustmark_trustmark_issuer` FOREIGN KEY (`trustmarkissuer_id`) REFERENCES `trustmark_issuer` (`trustmark_issuer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `trustmark_issuer` (
  `trustmark_issuer_id` uuid NOT NULL DEFAULT uuid(),
  `entity_id` uuid NOT NULL,
  `active` tinyint(1) NOT NULL,
  `trust_mark_token_validity_duration` varchar(255) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`trustmark_issuer_id`),
  KEY `entity_id` (`entity_id`),
  CONSTRAINT `fk_trustmark_issuer_entity` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
CREATE TABLE `trustmark_subject` (
  `trustmarksubject_id` uuid NOT NULL DEFAULT uuid(),
  `trustmark_id` uuid NOT NULL,
  `subject` varchar(255) DEFAULT NULL COMMENT 'Subject entity ID',
  `revoked` tinyint(1) DEFAULT 0 COMMENT 'If the trustmark is revoked',
  `granted` datetime DEFAULT NULL COMMENT 'When the trustmark was granted',
  `expires` datetime DEFAULT NULL COMMENT 'When the trustmark expires',
  `created_date` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified_date` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `created_by` varchar(255) NOT NULL,
  `last_modified_by` varchar(255) NOT NULL,
  `registration_id` uuid DEFAULT NULL,
  PRIMARY KEY (`trustmarksubject_id`),
  UNIQUE KEY `uk_trustmark_id_subject` (`trustmark_id`,`subject`),
  KEY `trustmark_id` (`trustmark_id`),
  KEY `fk_trustmarksubject_registration` (`registration_id`),
  CONSTRAINT `fk_trustmark_subject_trustmark` FOREIGN KEY (`trustmark_id`) REFERENCES `trustmark` (`trustmark_id`),
  CONSTRAINT `fk_trustmarksubject_registration` FOREIGN KEY (`registration_id`) REFERENCES `registrations` (`registration_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
