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

-- Find and remove the old foreign key constraint for module_id
-- MySQL auto-generates constraint names, so we need to find it dynamically
SET @constraint_name = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'trustmark'
      AND COLUMN_NAME = 'module_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
    );

SET @sql = CONCAT('ALTER TABLE `trustmark` DROP FOREIGN KEY `', IFNULL(@constraint_name, 'trustmark_ibfk_1'), '`');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE
PREPARE stmt;

-- Drop the module_id column
ALTER TABLE `trustmark`
    DROP COLUMN `module_id`;

-- Change trustmarkissuer_id from VARCHAR(255) to UUID and make it NOT NULL
-- The column was added in V3 as VARCHAR(255), now we change it to UUID
ALTER TABLE `trustmark`
    MODIFY COLUMN `trustmarkissuer_id` UUID NOT NULL;

-- Add foreign key constraint to trustmark_issuer table
ALTER TABLE `trustmark`
    ADD CONSTRAINT `fk_trustmark_trustmark_issuer`
        FOREIGN KEY (`trustmarkissuer_id`) REFERENCES `trustmark_issuer` (`trustmark_issuer_id`) ON DELETE RESTRICT;
