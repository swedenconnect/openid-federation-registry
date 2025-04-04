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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity class representing the 'module' table in the database. This class is a representation of a module entity and
 * extends the {@link BaseEntity}, inheriting auditing fields like created date, last modified date, created by, and
 * last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@Table(name = "module")
public class ModuleEntity extends BaseEntity {

  @Id
  @Column(name = "module_id", nullable = false, updatable = false)
  private UUID moduleId;

  @Size(max = 255)
  @NotNull
  @Column(name = "module_type", nullable = false)
  private String moduleType;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumns({
      @JoinColumn(name = "fk_type", referencedColumnName = "module_type", insertable = false, updatable = false),
      @JoinColumn(name = "fk_id", referencedColumnName = "module_id", insertable = false, updatable = false)
  })
  private List<SettingsEntity> settingsEntityList;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @OneToMany(mappedBy = "module", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<TrustMarkEntity> trustmarks;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_id", nullable = false)
  private EntityEntity entity;

  /**
   * Determines whether the module is of the specified types. Compares the module's type against the provided array of
   * {@link FkKeyType}.
   *
   * @param type an array of {@link FkKeyType} to check against the module's type
   * @return {@code true} if the module type matches any of the provided types, otherwise {@code false}
   */
  public boolean isOfType(final FkKeyType... type) {
    return Arrays.stream(type)
        .filter(t -> t.name().equals(this.moduleType))
        .map(t -> true)
        .findAny().orElse(false);

  }


  /**
   * Retrieves the {@link SettingsEntity} associated with the specified key. The method searches through the list of
   * settings entities and returns an optional containing the first entity matching the given key, if one exists.
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