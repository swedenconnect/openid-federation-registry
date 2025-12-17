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

-- Rename table from module to TrustanchorIntermediate
RENAME TABLE `module` TO `trustanchor_intermediate`;

-- Rename column from module_id to ta_im_id in TrustanchorIntermediate table
ALTER TABLE `trustanchor_intermediate`
    CHANGE COLUMN `module_id` `ta_im_id` UUID NOT NULL DEFAULT (UUID ());

drop table settings;