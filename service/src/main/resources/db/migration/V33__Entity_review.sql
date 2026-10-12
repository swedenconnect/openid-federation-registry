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

CREATE TABLE `entity_review` (
  `review_id` uuid NOT NULL,
  `registration_id` uuid NOT NULL,
  `instance_id` uuid NOT NULL,
  `status` varchar(20) NOT NULL,
  `started_by` varchar(255) NOT NULL,
  `started_at` datetime NOT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`review_id`),
  KEY `idx_entity_review_registration` (`registration_id`),
  KEY `idx_entity_review_instance_status` (`instance_id`, `status`),
  CONSTRAINT `fk_entity_review_registration` FOREIGN KEY (`registration_id`) REFERENCES `registrations` (`registration_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_entity_review_instance` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;

CREATE TABLE `entity_review_decision` (
  `decision_id` uuid NOT NULL,
  `review_id` uuid NOT NULL,
  `item_type` varchar(20) NOT NULL,
  `item_id` uuid NOT NULL,
  `step_index` int DEFAULT NULL,
  `step_name` varchar(255) DEFAULT NULL,
  `decision` varchar(20) NOT NULL,
  `reason` text DEFAULT NULL,
  `decided_by` varchar(255) NOT NULL,
  `decided_at` datetime NOT NULL,
  `created_date` datetime NOT NULL,
  `last_modified_date` datetime NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `last_modified_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`decision_id`),
  KEY `idx_entity_review_decision_review` (`review_id`),
  KEY `idx_entity_review_decision_item` (`item_type`, `item_id`),
  CONSTRAINT `fk_entity_review_decision_review` FOREIGN KEY (`review_id`) REFERENCES `entity_review` (`review_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;
