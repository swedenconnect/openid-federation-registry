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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.entity.registry.common.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The OrganizationEntity class represents an organization in the system. This class maps to the "organization" table in
 * the database. It extends the BaseEntity class, inheriting audit fields for created/modified dates and other user
 * metadata.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@Table(name = "organization")
public class OrganizationEntity extends BaseEntity {
  @Id
  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Size(max = 255)
  @Column(name = "org_number")
  private String orgNumber;

  @Size(max = 255)
  @Column(name = "org_name")
  private String orgName;

  @ManyToOne
  @JoinColumn(name = "instance_id", nullable = false)
  private InstanceEntity instance;

  @OneToMany(mappedBy = "organization")
  private List<ModuleEntity> module;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.DETACH, orphanRemoval = false)
  private List<PolicyEntity> policies = new ArrayList<>();

  @OneToMany(mappedBy = "organization", cascade = CascadeType.DETACH, orphanRemoval = false)
  private List<EntityEntity> entities;

  /**
   * Filters and retrieves a list of {@link ModuleEntity} objects associated with the given foreign key type.
   *
   * @param fkKeyType the foreign key type used to filter {@link ModuleEntity} objects based on their module type
   * @return a list of {@link ModuleEntity} objects where the module type matches the specified foreign key type
   */
  public List<ModuleEntity> getModuleByFKType(final FkKeyType fkKeyType) {
    return this.module
        .stream()
        .filter(moduleEntity -> moduleEntity.getModuleType().equals(fkKeyType.name()))
        .toList();

  }
}