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

-- The validity duration of an issued trust mark is set per trust mark instead of per trust mark issuer, so that trust
-- mark types of one issuer can have different lifetimes. Each trust mark keeps the duration of its issuer, and no
-- duration still means that the trust mark never expires.
ALTER TABLE `trustmark`
    ADD COLUMN `trust_mark_token_validity_duration` varchar(255) DEFAULT NULL
        COMMENT 'Trust mark token validity duration (ISO-8601), empty for no expiry';

-- Copying the value is not a change of the trust mark, so its last modified date is kept.
UPDATE `trustmark` t
    JOIN `trustmark_issuer` i ON i.`trustmark_issuer_id` = t.`trustmarkissuer_id`
SET t.`trust_mark_token_validity_duration` = i.`trust_mark_token_validity_duration`,
    t.`last_modified_date`                 = t.`last_modified_date`;

ALTER TABLE `trustmark_issuer`
    DROP COLUMN `trust_mark_token_validity_duration`;
