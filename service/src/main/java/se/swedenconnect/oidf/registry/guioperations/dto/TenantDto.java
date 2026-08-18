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
package se.swedenconnect.oidf.registry.guioperations.dto;

import java.util.List;

/**
 * One tenant (configured instance) the caller has rights on, with the organizations already registered under it.
 *
 * @param tenant the instance name, e.g. {@code "Swedenconnect"}
 * @param organizations the organizations registered under this tenant
 * @author Per Fredrik Plars
 */
public record TenantDto(String tenant, List<TenantOrganizationDto> organizations) {
}