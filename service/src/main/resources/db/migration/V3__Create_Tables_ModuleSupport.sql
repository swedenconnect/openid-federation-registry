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
)
    ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `instance`
(
    `instance_id`        VARCHAR(255) NOT NULL,
    `name`               VARCHAR(255) NULL,

    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`instance_id`)
)
    ENGINE = InnoDB;


CREATE TABLE IF NOT EXISTS `module`
(
    `module_id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `external_id`        varchar(255) not null,

    `instance_id`        VARCHAR(255),
    `module_type`        VARCHAR(255),
    `userdomain_id`      INT,

    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`module_id`)
)
    ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS `organization`
(
    `organization_id`    INT          NOT NULL AUTO_INCREMENT,
    `external_id`        varchar(255) not null,
    `entityid_filter`    VARCHAR(255) NULL comment 'What type of entityid that can be used. ex https://*.swedenconnect.se/*',
    `organization`       VARCHAR(255) NULL comment 'Org name that matches the or in JWT token. Ex DIGG,KALIXKOMMUN',

    `created_by`         VARCHAR(255) NOT NULL,
    `last_modified_by`   VARCHAR(255) NOT NULL,
    `created_date`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_modified_date` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`organization_id`)
)
    ENGINE = InnoDB;