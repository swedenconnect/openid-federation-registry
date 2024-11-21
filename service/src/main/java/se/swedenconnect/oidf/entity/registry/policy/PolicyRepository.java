/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * PolicyRepository is a JPA repository interface for accessing and performing
 * CRUD operations on {@link PolicyEntity} entities stored in the database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods
 * for standard data access layers.
 *
 * @author David Goldring
 */
public interface PolicyRepository extends JpaRepository<PolicyEntity, Long>{
  /**
   * Finds a policy by its name.
   *
   * @param name the unique name of the policy to be found
   * @return an Optional containing the policy if found, or an empty Optional if no policy with the given name exists
   */
  Optional<PolicyEntity> findByName(String name);

  /**
   * Resolve policy by its externalId
   * @param externalId UUID
   * @return an Optional containing the policy if found, or an empty Optional if no policy exist
   */
  Optional<PolicyEntity> findByExternalId(String externalId);
}
