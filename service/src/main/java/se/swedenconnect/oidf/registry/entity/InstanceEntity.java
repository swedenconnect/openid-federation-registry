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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

/**
 * The InstanceEntity class represents an instance in the system and maps to the "instance" table in the database. It
 * extends the BaseEntity class, inheriting fields for auditing purposes such as created/modified dates and user
 * metadata. This entity contains information regarding an instance and its associations with other entities.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@Table(name = "instance")
public class InstanceEntity extends BaseEntity {
  @Id
  @Column(name = "instance_id", nullable = false)
  private UUID instanceId;

  @Size(max = 255)
  @Column(name = "name")
  private String name;

  @OneToMany(mappedBy = "instance", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
  private Set<OrganizationEntity> organizations;

  @Column(name = "use_for_default_assignment", nullable = false)
  private boolean useForDefaultAssignment;

  /**
   * Adds an organization to the set of organizations associated with this instance and sets this instance as the parent
   * of the provided organization.
   *
   * @param organization the {@code OrganizationEntity} to be added to the instance's set of organizations.
   */
  public void addOrganization(final OrganizationEntity organization) {
    this.organizations.add(organization);
    organization.setInstance(this);
  }
}