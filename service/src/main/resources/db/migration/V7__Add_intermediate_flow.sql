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

-- Join table linking intermediates to registration flows (many-to-many).
CREATE TABLE `intermediate_flow` (
    `ta_im_id` uuid NOT NULL,
    `flow_id`   uuid NOT NULL,
    PRIMARY KEY (`ta_im_id`, `flow_id`),
    CONSTRAINT `fk_if_intermediate`
        FOREIGN KEY (`ta_im_id`) REFERENCES `trustanchor_intermediate` (`ta_im_id`),
    CONSTRAINT `fk_if_flow`
        FOREIGN KEY (`flow_id`) REFERENCES `registration_flow` (`flow_id`)
);
