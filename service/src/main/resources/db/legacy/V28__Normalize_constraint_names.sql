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

-- Give all auto-named foreign keys (<table>_ibfk_N, or just "1"/"2" depending on the environment)
-- explicit names, so that databases migrated from V1 get the same constraint names as databases
-- created from the V28 baseline. The existing name is looked up in information_schema, which makes
-- every statement below a no-op when the foreign key already has its final name.

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'entities'
                 AND `column_name` = 'organization_id'
                 AND `referenced_table_name` = 'organization'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_entities_organization', 'DO 0',
              CONCAT('ALTER TABLE `entities` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_entities_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'organization'
                 AND `column_name` = 'instance_id'
                 AND `referenced_table_name` = 'instance'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_organization_instance', 'DO 0',
              CONCAT('ALTER TABLE `organization` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_organization_instance` FOREIGN KEY (`instance_id`) REFERENCES `instance` (`instance_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'resolver'
                 AND `column_name` = 'entity_id'
                 AND `referenced_table_name` = 'entities'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_resolver_entity', 'DO 0',
              CONCAT('ALTER TABLE `resolver` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_resolver_entity` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'subordinate'
                 AND `column_name` = 'ta_im_id'
                 AND `referenced_table_name` = 'trustanchor_intermediate'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_subordinate_trustanchor_intermediate', 'DO 0',
              CONCAT('ALTER TABLE `subordinate` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_subordinate_trustanchor_intermediate` FOREIGN KEY (`ta_im_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'trustanchor_intermediate'
                 AND `column_name` = 'organization_id'
                 AND `referenced_table_name` = 'organization'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_ta_im_organization', 'DO 0',
              CONCAT('ALTER TABLE `trustanchor_intermediate` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_ta_im_organization` FOREIGN KEY (`organization_id`) REFERENCES `organization` (`organization_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'trustanchor_intermediate'
                 AND `column_name` = 'entity_id'
                 AND `referenced_table_name` = 'entities'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_ta_im_entity', 'DO 0',
              CONCAT('ALTER TABLE `trustanchor_intermediate` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_ta_im_entity` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'trustmark_issuer'
                 AND `column_name` = 'entity_id'
                 AND `referenced_table_name` = 'entities'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_trustmark_issuer_entity', 'DO 0',
              CONCAT('ALTER TABLE `trustmark_issuer` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_trustmark_issuer_entity` FOREIGN KEY (`entity_id`) REFERENCES `entities` (`entity_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_fk = (SELECT `constraint_name`
               FROM `information_schema`.`key_column_usage`
               WHERE `table_schema` = DATABASE()
                 AND `table_name` = 'trustmark_subject'
                 AND `column_name` = 'trustmark_id'
                 AND `referenced_table_name` = 'trustmark'
               LIMIT 1);
SET @ddl = IF(@old_fk IS NULL OR @old_fk = 'fk_trustmark_subject_trustmark', 'DO 0',
              CONCAT('ALTER TABLE `trustmark_subject` DROP FOREIGN KEY `', @old_fk, '`, ',
                     'ADD CONSTRAINT `fk_trustmark_subject_trustmark` FOREIGN KEY (`trustmark_id`) REFERENCES `trustmark` (`trustmark_id`)'));
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @old_fk = NULL;
SET @ddl = NULL;
