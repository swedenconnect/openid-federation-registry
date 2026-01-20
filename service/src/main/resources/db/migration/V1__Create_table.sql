/*
 * Copyright 2025-2026 Sweden Connect
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

-- --------------------------------------------------------

--
-- Tabellstruktur `entities`
--

CREATE TABLE IF NOT EXISTS `entities`
(
    `entity_id`            uuid         NOT NULL                              DEFAULT uuid(),
    `organization_id`      uuid         NOT NULL,
    `entity_type`          varchar(20)  NOT NULL,
    `policy_id`            uuid                                               DEFAULT NULL,
    `metadata`             longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'Metadata for FederationEntity and HostedEntity' CHECK (json_valid(`metadata`)),
    `jwks`                 text                                               DEFAULT NULL COMMENT 'JWKSet for SubordinateEntity',
    `subject`              varchar(255)                                       DEFAULT NULL COMMENT 'Subject',
    `issuer`               varchar(255)                                       DEFAULT NULL COMMENT 'Issuer',
    `crit`                 text                                               DEFAULT NULL COMMENT 'List of crit claims',
    `metadata_policy_crit` text                                               DEFAULT NULL COMMENT 'List of metadata_policy_crit',
    `created_date`         datetime     NOT NULL                              DEFAULT current_timestamp(),
    `last_modified_date`   datetime     NOT NULL                              DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created_by`           varchar(255) NOT NULL,
    `last_modified_by`     varchar(255) NOT NULL,
    PRIMARY KEY (`entity_id`),
    KEY `organization_id` (`organization_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Tabellstruktur `instance`
--

CREATE TABLE IF NOT EXISTS `instance`
(
    `instance_id`                uuid         NOT NULL,
    `name`                       varchar(255)          DEFAULT NULL,
    `use_for_default_assignment` varchar(255)          DEFAULT NULL,
    `created_by`                 varchar(255) NOT NULL,
    `last_modified_by`           varchar(255) NOT NULL,
    `created_date`               datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date`         datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Tabellstruktur `organization`
--

CREATE TABLE IF NOT EXISTS `organization`
(
    `organization_id`    uuid         NOT NULL DEFAULT uuid(),
    `instance_id`        uuid         NOT NULL,
    `org_number`         varchar(255) NOT NULL COMMENT 'Org id that matches the claim in JWT token.',
    `org_name`           varchar(255)          DEFAULT NULL COMMENT 'Org name that matches the claim in JWT token.',
    `created_by`         varchar(255) NOT NULL,
    `last_modified_by`   varchar(255) NOT NULL,
    `created_date`       datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date` datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`organization_id`),
    UNIQUE KEY `org_number` (`org_number`),
    KEY `instance_id` (`instance_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Tabellstruktur `policies`
--

CREATE TABLE IF NOT EXISTS `policies`
(
    `policy_id`          uuid         NOT NULL DEFAULT uuid(),
    `organization_id`    uuid         NOT NULL,
    `name`               varchar(255)          DEFAULT NULL,
    `policy`             text                  DEFAULT NULL,
    `created_date`       datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date` datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created_by`         varchar(255) NOT NULL,
    `last_modified_by`   varchar(255) NOT NULL,
    PRIMARY KEY (`policy_id`),
    KEY `organization_id` (`organization_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Tabellstruktur `resolver`
--

CREATE TABLE IF NOT EXISTS `resolver`
(
    `resolver_id`                 uuid         NOT NULL DEFAULT uuid(),
    `entity_id`                   uuid         NOT NULL,
    `active`                      tinyint(1)   NOT NULL,
    `resolve_response_duration`   varchar(255) NOT NULL,
    `step_cached_value_threshold` int(11)      NOT NULL,
    `trust_anchor`                varchar(255) NOT NULL,
    `trusted_keys`                text         NOT NULL,
    `step_retry_duration`         varchar(255) NOT NULL,
    `created_by`                  varchar(255) NOT NULL,
    `last_modified_by`            varchar(255) NOT NULL,
    `created_date`                datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date`          datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`resolver_id`),
    KEY `entity_id` (`entity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Tabellstruktur `trustanchor_intermediate`
--

CREATE TABLE IF NOT EXISTS `trustanchor_intermediate`
(
    `ta_im_id`                           uuid         NOT NULL DEFAULT uuid(),
    `organization_id`                    uuid         NOT NULL,
    `entity_id`                          uuid         NOT NULL,
    `module_type`                        varchar(20)           DEFAULT NULL,
    `active`                             tinyint(1)            DEFAULT 1 COMMENT 'If module is active',
    `trust_mark_issuers`                 varchar(255)          DEFAULT NULL COMMENT 'List of Trust mark issuers for TrustAnchor',
    `trust_mark_token_validity_duration` varchar(255)          DEFAULT NULL COMMENT 'Trust mark token validity duration for TrustmarkIssuer',
    `created_date`                       datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date`                 datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created_by`                         varchar(255) NOT NULL,
    `last_modified_by`                   varchar(255) NOT NULL,
    PRIMARY KEY (`ta_im_id`),
    KEY `organization_id` (`organization_id`),
    KEY `entity_id` (`entity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;


-- --------------------------------------------------------

--
-- Tabellstruktur `trustmark`
--

CREATE TABLE IF NOT EXISTS `trustmark`
(
    `trustmark_id`        uuid         NOT NULL DEFAULT uuid(),
    `trustmarkissuer_id`  uuid         NOT NULL,
    `trustmark_entity_id` varchar(255)          DEFAULT NULL COMMENT 'Trustmark entity ID',
    `logo_uri`            varchar(512)          DEFAULT NULL COMMENT 'URL to logotype image',
    `ref_uri`             varchar(512)          DEFAULT NULL COMMENT 'Reference URL',
    `delegation`          text                  DEFAULT NULL COMMENT 'Delegation JWT according to OIDF specification',
    `created_date`        datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date`  datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created_by`          varchar(255) NOT NULL,
    `last_modified_by`    varchar(255) NOT NULL,
    PRIMARY KEY (`trustmark_id`),
    KEY `fk_trustmark_trustmark_issuer` (`trustmarkissuer_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

-- --------------------------------------------------------

--
-- Tabellstruktur `trustmark_issuer`
--

CREATE TABLE IF NOT EXISTS `trustmark_issuer`
(
    `trustmark_issuer_id`                uuid         NOT NULL DEFAULT uuid(),
    `entity_id`                          uuid         NOT NULL,
    `active`                             tinyint(1)   NOT NULL,
    `trust_mark_token_validity_duration` varchar(255) NOT NULL,
    `created_by`                         varchar(255) NOT NULL,
    `last_modified_by`                   varchar(255) NOT NULL,
    `created_date`                       datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date`                 datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    PRIMARY KEY (`trustmark_issuer_id`),
    KEY `entity_id` (`entity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;


-- --------------------------------------------------------

--
-- Tabellstruktur `trustmark_subject`
--

CREATE TABLE IF NOT EXISTS `trustmark_subject`
(
    `trustmarksubject_id` uuid         NOT NULL DEFAULT uuid(),
    `trustmark_id`        uuid         NOT NULL,
    `subject`             varchar(255)          DEFAULT NULL COMMENT 'Subject entity ID',
    `revoked`             tinyint(1)            DEFAULT 0 COMMENT 'If the trustmark is revoked',
    `granted`             datetime              DEFAULT NULL COMMENT 'When the trustmark was granted',
    `expires`             datetime              DEFAULT NULL COMMENT 'When the trustmark expires',
    `created_date`        datetime     NOT NULL DEFAULT current_timestamp(),
    `last_modified_date`  datetime     NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
    `created_by`          varchar(255) NOT NULL,
    `last_modified_by`    varchar(255) NOT NULL,
    PRIMARY KEY (`trustmarksubject_id`),
    KEY `trustmark_id` (`trustmark_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_uca1400_ai_ci;

--
-- Restriktioner för dumpade tabeller
--

--
-- Restriktioner för tabell `entities`
--
ALTER TABLE `entities`
    ADD CONSTRAINT `entities_ibfk_1` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`);

--
-- Restriktioner för tabell `organization`
--
ALTER TABLE `organization`
    ADD CONSTRAINT `organization_ibfk_1` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`);

--
-- Restriktioner för tabell `policies`
--
ALTER TABLE `policies`
    ADD CONSTRAINT `policies_ibfk_1` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`);

--
-- Restriktioner för tabell `resolver`
--
ALTER TABLE `resolver`
    ADD CONSTRAINT `resolver_ibfk_1` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`);

--
-- Restriktioner för tabell `trustanchor_intermediate`
--
ALTER TABLE `trustanchor_intermediate`
    ADD CONSTRAINT `trustanchor_intermediate_ibfk_1` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`),
    ADD CONSTRAINT `trustanchor_intermediate_ibfk_2` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`);

--
-- Restriktioner för tabell `trustmark`
--
ALTER TABLE `trustmark`
    ADD CONSTRAINT `fk_trustmark_trustmark_issuer` FOREIGN KEY (`trustmarkissuer_id`) REFERENCES `trustmark_issuer` (`trustmark_issuer_id`);

--
-- Restriktioner för tabell `trustmark_issuer`
--
ALTER TABLE `trustmark_issuer`
    ADD CONSTRAINT `trustmark_issuer_ibfk_1` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`);

--
-- Restriktioner för tabell `trustmark_subject`
--
ALTER TABLE `trustmark_subject`
    ADD CONSTRAINT `trustmark_subject_ibfk_1` FOREIGN KEY (`trustmark_id`) REFERENCES `trustmark` (`trustmark_id`);
COMMIT;



-- _____________________________________________________________________________________
-- Instance table

CREATE TABLE IF NOT EXISTS `instance`
(
    `instance_id`                UUID         NOT NULL,
    `name`                       VARCHAR(255) NULL,
    `use_for_default_assignment` VARCHAR(255) NULL,
    `created_by`                 VARCHAR(255) NOT NULL,
    `last_modified_by`           VARCHAR(255) NOT NULL,
    `created_date`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`instance_id`)
) ENGINE = InnoDB;

-- Organization table
CREATE TABLE IF NOT EXISTS `organization`
(
    `organization_id`    UUID                NOT NULL DEFAULT (UUID()),
    `instance_id`        UUID                NOT NULL,
    `org_number`         VARCHAR(255) UNIQUE NOT NULL COMMENT 'Org id that matches the claim in JWT token.',
    `org_name`           VARCHAR(255)        NULL COMMENT 'Org name that matches the claim in JWT token.',
    `created_by`         VARCHAR(255)        NOT NULL,
    `last_modified_by`   VARCHAR(255)        NOT NULL,
    `created_date`       DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`organization_id`),
    FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`) ON DELETE RESTRICT

) ENGINE = InnoDB;


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


-- Entities table
CREATE TABLE IF NOT EXISTS entities
(
    `entity_id`          UUID         NOT NULL DEFAULT (UUID()),
    `organization_id`    UUID         NOT NULL,
    `entity_type`        VARCHAR(20)  NOT NULL,
    `policy_id` UUID NULL,
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`entity_id`),
    FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`) ON DELETE RESTRICT
) ENGINE = InnoDB;


-- Module table
CREATE TABLE IF NOT EXISTS `module`
(
    `module_id`          UUID         NOT NULL DEFAULT (UUID()),
    `organization_id`    UUID         NOT NULL,
    `entity_id`   UUID NOT NULL,
    `module_type` VARCHAR(20),
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`module_id`),
    FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`) ON DELETE RESTRICT,
    FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`) ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `trustmark`
(
    `trustmark_id`       UUID         NOT NULL DEFAULT (UUID()),
    `module_id`          UUID         NOT NULL,
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`trustmark_id`),
    FOREIGN KEY (`module_id`) REFERENCES `module` (`module_id`) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Trustmark subject table
CREATE TABLE IF NOT EXISTS trustmark_subject
(
    `trustmarksubject_id` UUID         NOT NULL DEFAULT (UUID()),
    `trustmark_id`        UUID         NOT NULL,
    `created_date`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_by`          VARCHAR(255) NOT NULL,
    `last_modified_by`    VARCHAR(255) NOT NULL,
    PRIMARY KEY (`trustmarksubject_id`),
    FOREIGN KEY (`trustmark_id`) REFERENCES `trustmark` (`trustmark_id`) ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Settings table
CREATE TABLE IF NOT EXISTS settings
(
    `property_id`        BIGINT       NOT NULL AUTO_INCREMENT,
    `fk_id`              VARCHAR(255) NOT NULL,
    `fk_type`            VARCHAR(255) NOT NULL,
    `data_key`           VARCHAR(255) NOT NULL,
    `description`        VARCHAR(255),
    `validation`         VARCHAR(255),
    `data_type`          VARCHAR(255) COMMENT 'Datatype of data_value,Text,BOOLEAN,NUMERIC',
    `data_value`         TEXT,
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`property_id`)
) ENGINE = InnoDB;

-- Policies table
CREATE TABLE IF NOT EXISTS policies
(
    policy_id UUID NOT NULL DEFAULT (UUID()),
    organization_id    UUID         NOT NULL,
    created_date       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    PRIMARY KEY (`policy_id`),
    FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`) ON DELETE RESTRICT

) ENGINE = InnoDB;




