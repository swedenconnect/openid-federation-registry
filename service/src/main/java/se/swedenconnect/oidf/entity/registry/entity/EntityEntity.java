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
package se.swedenconnect.oidf.entity.registry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import se.swedenconnect.oidf.entity.registry.common.BaseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityDao is a JPA entity representing a database table for storing entities as JSON objects with the objects Subject
 * value as key.
 *
 * @author David Goldring
 */
@Getter
@Setter
@Entity
@ToString(callSuper = true)
@Table(name = "entities")
public class EntityEntity extends BaseEntity {
  @Id
  @Column(name = "entity_id", unique = true, updatable = false, nullable = false)
  private UUID entityId;

  @Column(name = "entity_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private EntityKeyType entityType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @OneToMany(mappedBy = "entity")
  private List<ModuleEntity> modules;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "fk_type", referencedColumnName = "entity_type", insertable = false, updatable = false),
      @JoinColumn(name = "fk_id", referencedColumnName = "entity_id", insertable = false, updatable = false)
  })
  private List<SettingsEntity> settingsEntityList;

  /**
   * Searches the settingsEntityList for a {@link SettingsEntity} that matches the given key.
   *
   * @param key the key of the {@link SettingsEntity} to search for
   * @return an {@link Optional} containing the matching {@link SettingsEntity} if found, or an empty {@link Optional}
   *     if no match is found
   */
  public Optional<SettingsEntity> getSettingsEntity(final String key) {
    return this.settingsEntityList.stream()
        .filter(s -> s.getKey().equals(key))
        .findFirst();
  }

}