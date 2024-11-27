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
package se.swedenconnect.oidf.entity.registry.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * EntityRepository is a JPA repository interface for accessing and performing
 * CRUD operations on {@link EntityEntity} entities stored in the database.
 * <p>
 * It extends the {@link JpaRepository} interface which provides JPA related methods
 * for standard data access layers.
 *
 * @author David Goldring
 */
public interface EntityRepository extends JpaRepository<EntityEntity, Long> {
  /**
   * Resolve EntityEntity by its externalId
   * @param externalId UUID
   * @return an Optional containing the EntityEntity if found, or an empty Optional if no
   * EntityEntity exist
   */
  Optional<EntityEntity> findByExternalId(String externalId);

  /**
   * Find by Entity Issuer
   * @param issuer Issuer
   * @return List of Entity for this issuer
   */
  List<EntityEntity> findByIssuer(String issuer);
}
