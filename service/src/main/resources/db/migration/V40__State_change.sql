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

CREATE SEQUENCE `state_change_seq` START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE `state_change` (
  `seq` bigint NOT NULL,
  `instance_id` uuid NOT NULL,
  `change_key` varchar(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `op` varchar(10) NOT NULL COMMENT 'upsert or delete',
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`seq`),
  KEY `idx_state_change_instance_seq` (`instance_id`, `seq`),
  KEY `idx_state_change_instance_key_seq` (`instance_id`, `change_key`, `seq`),
  CONSTRAINT `fk_state_change_instance` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `state_pruned` (
  `instance_id` uuid NOT NULL,
  `pruned_seq` bigint NOT NULL,
  PRIMARY KEY (`instance_id`),
  CONSTRAINT `fk_state_pruned_instance` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `state_record_config` (
  `id` tinyint NOT NULL,
  `record_version` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

INSERT INTO `state_record_config` (`id`, `record_version`) VALUES (1, NULL);
