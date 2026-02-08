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

package se.swedenconnect.oidf.registry.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.registry.entity.converters.StringListConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity class representing the 'module' table in the database. This class is a representation of a TaIm entity and
 * extends the {@link BaseEntity}, inheriting auditing fields like created date, last modified date, created by, and
 * last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@Table(name = "TrustanchorIntermediate")
public class TaImEntity extends BaseEntity {
  /**
   * Enum representing the type of a module in the system. Used to categorize modules into specific roles or
   * functionalities.
   *
   * INTERMEDIATE - Represents an intermediate module, typically used for subordinate purposes within a trust
   * hierarchy.
   *
   * TRUSTANCHOR - Represents a trust anchor module, a root entity within a trust hierarchy.
   */
  public enum Type {
    INTERMEDIATE,
    TRUSTANCHOR
  }

  @Id
  @Column(name = "ta_im_id", nullable = false, updatable = false)
  private UUID taImId;

  @NotNull
  @Column(name = "module_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private Type moduleType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_id", nullable = false)
  private EntityEntity entity;

  @OneToMany(mappedBy = "taIm", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SubordinateEntity> subordinates = new ArrayList<>();

  // Columns for module data (used by TrustAnchor, Resolver, TrustmarkIssuer)

  @Column(name = "active")
  private Boolean active;

  @Column(name = "trust_mark_issuers")
  @Convert(converter = StringListConverter.class)
  private List<String> trustMarkIssuers;

  /**
   * Determines whether the module is of the specified types. Compares the module's type against the provided array of
   * {@link FkKeyType}.
   *
   * @param type an array of {@link FkKeyType} to check against the module's type
   * @return {@code true} if the module type matches any of the provided types, otherwise {@code false}
   */
  public boolean isOfType(final Type type) {
    return this.moduleType.equals(type);

  }

}
