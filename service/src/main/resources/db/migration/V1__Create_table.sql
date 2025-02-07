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


-- Entities table
CREATE TABLE IF NOT EXISTS entities
(
    id                 bigint       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    external_id        varchar(255) NOT NULL,
    entity             TEXT         NOT NULL,
    issuer             varchar(255) NOT NULL,
    subject            varchar(255) NOT NULL,
    created_date       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    CONSTRAINT entities_const UNIQUE (issuer, subject)
) ENGINE=InnoDB;

-- Policies table
CREATE TABLE IF NOT EXISTS policies
(
    id                 bigint       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    external_id        varchar(255) NOT NULL,
    name               varchar(255) NOT NULL,
    policy             TEXT         NOT NULL,
    created_date       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by         VARCHAR(255) NOT NULL,
    last_modified_by   VARCHAR(255) NOT NULL,
    CONSTRAINT policies_const UNIQUE (external_id)
) ENGINE=InnoDB;

-- Trustmark subject table
CREATE TABLE IF NOT EXISTS trustmark_subject
(
    id                    bigint       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    external_id           varchar(255) NOT NULL,
    issuer                varchar(255) NOT NULL,
    subject               varchar(255) NOT NULL,
    trustmark_id          varchar(255) NOT NULL,
    trustmarksubject_json TEXT         NOT NULL,
    created_date          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by            VARCHAR(255) NOT NULL,
    last_modified_by      VARCHAR(255) NOT NULL,
    CONSTRAINT trustmark_subject_const UNIQUE (issuer, trustmark_id, subject)
) ENGINE=InnoDB;

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
) ENGINE=InnoDB;

-- Instance table
CREATE TABLE IF NOT EXISTS `instance`
(
    `instance_id`        VARCHAR(255) NOT NULL,
    `name`               VARCHAR(255) NULL,
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`instance_id`)
) ENGINE=InnoDB;

-- Organization table
CREATE TABLE IF NOT EXISTS `organization`
(
    `organization_id`    BIGINT       NOT NULL AUTO_INCREMENT,
    `external_id`        varchar(255) NOT NULL,
    `entityid_filter`    VARCHAR(255) NULL COMMENT 'What type of entityid that can be used. ex https://*.swedenconnect.se/*',
    `org_id`             VARCHAR(255) NULL COMMENT 'Org id that matches the claim in JWT token.',
    `org_name`           VARCHAR(255) NULL COMMENT 'Org nam that matches the claim in JWT token. Ex DIGG,KALIXKOMMUN',
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`organization_id`)
) ENGINE=InnoDB;

-- Organization instance link table
CREATE TABLE IF NOT EXISTS `organization_instance_link`
(
    `organization_id`    BIGINT       NOT NULL,
    `instance_id`        VARCHAR(255) NOT NULL,
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`organization_id`, `instance_id`),
    CONSTRAINT fk_org_instance_organization FOREIGN KEY (`organization_id`)
        REFERENCES `organization` (`organization_id`) ON DELETE CASCADE,
    CONSTRAINT fk_org_instance_instance FOREIGN KEY (`instance_id`)
        REFERENCES `instance` (`instance_id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Module table
CREATE TABLE IF NOT EXISTS `module`
(
    `module_id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `external_id`        varchar(255) NOT NULL,
    `instance_id`        VARCHAR(255) NOT NULL,
    `module_type`        VARCHAR(255),
    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`module_id`),
    FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`)
) ENGINE=InnoDB;