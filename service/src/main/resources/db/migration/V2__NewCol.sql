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

ALTER TABLE `entities`
    ADD COLUMN `ec_location` VARCHAR(255) DEFAULT NULL COMMENT 'Location where the actual entity statement is placed',
    ADD COLUMN `ec_location_automatic` TINYINT(1) NOT NULL DEFAULT 0
COMMENT
'When true, eclocation will be loaded from the hosted entity with the same issuer entityid',
    ADD COLUMN `trustmarksources` TEXT DEFAULT NULL COMMENT
'Trustmark sources that can be used to include trustmarks';


