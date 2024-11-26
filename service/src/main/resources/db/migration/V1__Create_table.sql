/*
 * Copyright 2024 Sweden Connect.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE if not exists entities (
    id bigint not null PRIMARY KEY AUTO_INCREMENT,
    external_id varchar(255) not null,
    entity TEXT  not null,
    issuer varchar(255)  not null,
    subject varchar(255)  not null
) engine=InnoDB;

alter table if exists entities
   add constraint entities_const unique (issuer, subject);

CREATE TABLE if not exists policies (
    id bigint not null PRIMARY KEY AUTO_INCREMENT,
    external_id varchar(255) not null,
    name varchar(255) not null,
    policy TEXT not null
) engine=InnoDB;

alter table if exists policies
   add constraint policies_const unique (external_id);

CREATE TABLE if not exists trustmark_subject (
    id bigint not null PRIMARY KEY AUTO_INCREMENT,
    external_id varchar(255) not null,
    issuer varchar(255) not null,
    subject varchar(255) not null,
    trustmark_id varchar(255)  not null,
    trustmarksubject_json TEXT  not null
) engine=InnoDB;

alter table if exists trustmark_subject
   add constraint trustmark_subject_const unique (issuer, trustmark_id, subject);