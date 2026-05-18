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
package se.swedenconnect.oidf.registry.infrastructure.auth.domain;

import java.io.Serializable;

/**
 * Organizational orgInfo about a specific org.
 *
 * @param orgNumber Organization Number
 * @param orgName OrganizationName
 * @param entityPrefix EntityPrefix ex https://www.digg.se/oidf/
 * @param functionGroup Optional function group identifier used for instance placement matching
 * @author Felix Hellman
 */
public record OrganizationRecord(String orgNumber, String orgName, String entityPrefix, String functionGroup)
    implements Serializable {
}
