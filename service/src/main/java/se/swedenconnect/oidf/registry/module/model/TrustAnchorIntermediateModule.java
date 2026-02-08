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
package se.swedenconnect.oidf.registry.module.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.StringListConverter;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity class representing the 'module' table in the database. This class is a representation of a
 * TrustAnchorIntermediateModule entity and extends the {@link BaseEntity}, inheriting auditing fields like created
 * date, last modified date, created by, and last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@Table(name = "TrustanchorIntermediate")
public class TrustAnchorIntermediateModule extends BaseEntity {
  @Id
  @Column(name = "ta_im_id", nullable = false, updatable = false)
  private UUID taImId;

  @NotNull
  @Column(name = "module_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private ModuleType moduleType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_id", nullable = false)
  private FederationEntity entity;

  @OneToMany(mappedBy = "taIm", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Subordinate> subordinates = new ArrayList<>();

  // Columns for module data (used by TrustAnchor, Resolver, TrustmarkIssuer)

  @Column(name = "active")
  private Boolean active;

  @Column(name = "trust_mark_issuers")
  @Convert(converter = StringListConverter.class)
  private List<String> trustMarkIssuers;

  /**
   * Determines whether the module is of the specified types. Compares the module's type against the provided
   * {@link ModuleType}.
   *
   * @param type the {@link ModuleType} to check against the module's type
   * @return {@code true} if the module type matches the provided type, otherwise {@code false}
   */
  public boolean isOfType(final ModuleType type) {
    return this.moduleType.equals(type);

  }

}
