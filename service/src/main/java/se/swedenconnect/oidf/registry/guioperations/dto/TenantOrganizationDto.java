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

/**
 * One organization registered under a tenant, as returned by the {@code /tenants} endpoint.
 *
 * @param orgNumber the organization number
 * @param orgName the organization name
 * @param entityPrefix entityPrefix for this organization ex https://www.ppm.nu/oidf, {@code null} if the
 *     organization is not yet placed on any instance
 * @author Per Fredrik Plars
 */
public record TenantOrganizationDto(String orgNumber, String orgName, String entityPrefix) {
}