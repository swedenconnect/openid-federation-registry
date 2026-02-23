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

package se.swedenconnect.oidf.registry.policy.mapper;

import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.policy.dto.PolicyDto;
import se.swedenconnect.oidf.registry.policy.model.Policy;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for converting DTO objects to Policy objects.
 *
 * @author Per Fredrik Plars
 */
public final class DtoToPolicyMapper {
  private DtoToPolicyMapper() {
  }

  /**
   * Converts PolicyDto to Policy.
   *
   * @param id the policy ID
   * @param dto the policy DTO
   * @param organization the organization entity
   * @return the policy entity
   */
  public static Policy toEntity(final UUID id,
      final PolicyDto dto,
      final Organization organization) {
    final Policy entity = new Policy();
    entity.setPolicyId(id);
    entity.setOrganization(organization);
    updateEntity(entity, dto);
    return entity;
  }

  /**
   * Updates Policy with PolicyDto data.
   *
   * @param entity the policy entity
   * @param dto the policy DTO
   */
  public static void updateEntity(final Policy entity, final PolicyDto dto) {
    entity.setName(dto.getName());
    final Map<String, Object> policy = dto.getPolicy() != null ? dto.getPolicy() : Collections.emptyMap();
    entity.setPolicy(policy);
  }
}
