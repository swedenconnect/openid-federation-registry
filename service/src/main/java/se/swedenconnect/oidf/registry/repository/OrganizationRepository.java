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
package se.swedenconnect.oidf.registry.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * OrganizationRepository is a JPA repository interface for performing database operations on {@link OrganizationEntity}
 * objects. This repository provides standard CRUD functionality and enables the use of custom queries to manage and
 * access organization data in the system. The entity is identified by a {@link UUID}.
 *
 * @author Per Fredrik Plars
 */
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
  /**
   * Retrieves an organization entity based on its organization number.
   *
   * @param orgNumber the organization number used to identify the {@link OrganizationEntity}
   * @return an {@link Optional} containing the found {@link OrganizationEntity}, or an empty {@link Optional} if no
   *     organization is found with the given number
   */
  Optional<OrganizationEntity> findByOrgNumber(String orgNumber);

}
