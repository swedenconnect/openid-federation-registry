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

package se.swedenconnect.oidf.registry.subordinate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Subordinate.
 *
 * @author Per Fredrik Plars
 */
@Repository
public interface SubordinateRepository extends JpaRepository<Subordinate, UUID> {

  /**
   * Finds subordinate by orgid and entityidentifier
   *
   * @param orgNumber Orgnumber
   * @param entityidentifier entityidentifier
   * @return List of Subordinate
   */
  @Query("SELECT s FROM Subordinate s JOIN s.taIm m JOIN m.organization o "
      + "WHERE o.orgNumber = :orgNumber AND s.entityidentifier = :entityidentifier ")
  List<Subordinate> findByOrgNumberAndEntityidentifier(
      @Param("orgNumber") String orgNumber, @Param("entityidentifier") String entityidentifier);

  /**
   * Finds subordinate by entityidentifier
   *
   * @param entityidentifier entityidentifier
   * @param taImId taImId
   * @return subordinate
   */
  Optional<Subordinate> findByEntityidentifierAndTaIm_TaImId(@Param("entityidentifier") String entityidentifier,
      @Param("taImId") UUID taImId);

}
