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

-- The trust mark issuers of a trust anchor change from a list of entity identifiers to a list of objects
-- {issuer, auto, trustMarkTypes}, stored as JSON in the same column. With auto set, all trust marks of the issuer
-- (a trust mark issuer of the same organization) are included as they are when the configuration is fetched, and
-- without auto the trust mark types are listed. A list of objects does not fit in 255 characters.
ALTER TABLE `trustanchor_intermediate`
    MODIFY COLUMN `trust_mark_issuers` TEXT DEFAULT NULL
        COMMENT 'Trust mark issuers of a TrustAnchor, a JSON list of {issuer, auto, trustMarkTypes}';

-- Only a trust anchor has trust mark issuers, and a value that is not JSON cannot be read.
UPDATE `trustanchor_intermediate`
SET `trust_mark_issuers` = NULL
WHERE `trust_mark_issuers` IS NOT NULL
  AND (`module_type` IS NULL OR `module_type` <> 'TRUSTANCHOR' OR NOT JSON_VALID(`trust_mark_issuers`));

-- The issuers that were listed on a trust anchor were entity identifiers of trust mark issuers and had no trust mark
-- types, so they become auto entries. A repeated issuer is kept once.
UPDATE `trustanchor_intermediate` m
    JOIN (SELECT t.`ta_im_id`,
                 JSON_ARRAYAGG(JSON_OBJECT('issuer', t.`issuer`, 'auto', JSON_EXTRACT('true', '$'),
                                           'trustMarkTypes', JSON_ARRAY())) AS `converted`
          FROM (SELECT DISTINCT m2.`ta_im_id`, j.`issuer`
                FROM `trustanchor_intermediate` m2,
                     JSON_TABLE(m2.`trust_mark_issuers`, '$[*]' COLUMNS (`issuer` varchar(255) PATH '$')) j
                WHERE m2.`trust_mark_issuers` IS NOT NULL
                  AND j.`issuer` IS NOT NULL
                  AND j.`issuer` <> '') t
          GROUP BY t.`ta_im_id`) c ON c.`ta_im_id` = m.`ta_im_id`
SET m.`trust_mark_issuers` = c.`converted`;
