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
package se.swedenconnect.oidf.registry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PolicyDao is a JPA entity representing a database table for storing policies as JSON objects with the policy id as
 * key.
 *
 * @author David Goldring
 */
@Getter
@Setter
@Entity
@Table(name = "policies")
@FilterDef(name = "fkTypeFilter", parameters = @ParamDef(name = "POLICIES", type = String.class))

public class PolicyEntity extends BaseEntity {
  @Id
  @Column(name = "policy_id", unique = true, updatable = false, nullable = false)
  private UUID policyId;

  @Column(name = "organization_id", insertable = false, updatable = false, nullable = false)
  private UUID organizationId;

  @ManyToOne
  @JoinColumn(name = "organization_id")
  private OrganizationEntity organization;

  @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
  @JoinColumn(name = "fk_id", referencedColumnName = "policy_id", insertable = false, updatable = false)
  @Filter(name = "fkTypeFilter", condition = "fk_type = :fkTypeParam")
  private List<SettingsEntity> settingsEntityList;

  /**
   * Retrieves the {@link SettingsEntity} associated with the specified key. The method searches through the list of
   * settings entities and returns an optional containing the first entity matching the given key if one exists.
   *
   * @param key the key to search for in the list of settings entities
   * @return an {@link Optional} containing the matching {@link SettingsEntity} if found, otherwise an empty
   *     {@link Optional}
   */
  public Optional<SettingsEntity> getSettingsEntity(final String key) {
    return this.settingsEntityList.stream()
        .filter(s -> s.getKey().equals(key))
        .findFirst();
  }

}


