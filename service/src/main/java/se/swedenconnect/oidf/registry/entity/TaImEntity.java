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
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

  @Enumerated(EnumType.STRING)
  @NotNull
  @Column(name = "module_type", nullable = false)
  private Type moduleType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_id", nullable = false)
  private EntityEntity entity;

  // Columns for module data (used by TrustAnchor, Resolver, TrustmarkIssuer)

  @Column(name = "active")
  private Boolean active;

  @Column(name = "trust_mark_issuers")
  private String trustMarkIssuers;

  @Column(name = "critical")
  private String critical;

  /**
   * Retrieves a list of trust mark issuers by splitting the stored string of trust mark issuers using a comma as a
   * delimiter. If the stored string is empty, returns null.
   *
   * @return a list of trust mark issuer strings, or null if no trust mark issuers are available
   */
  public List<String> getTrustMarkIssuers() {
    if (this.trustMarkIssuers == null) {
      return Collections.emptyList();
    }
    return this.trustMarkIssuers.isEmpty() ? null : List.of(this.trustMarkIssuers.split(","));
  }

  /**
   * Retrieves a list of critical components by splitting the stored string using a comma as a delimiter. If the stored
   * string is empty, returns null.
   *
   * @return a list of critical component strings, or null if no critical components are available
   */
  public List<String> getCritical() {
    if (this.critical == null) {
      return Collections.emptyList();
    }
    return this.critical.isEmpty() ? null : List.of(this.critical.split(","));
  }

  /**
   * Sets the trust mark issuers for the system. The provided list of issuer names is processed and stored as a
   * comma-separated string. If the provided list is empty, the internal value is set to null.
   *
   * @param trustMarkIssuers a list of trust mark issuer names to be stored. If the list is empty, the value will be
   *     set to null.
   */
  public void setTrustMarkIssuers(final List<String> trustMarkIssuers) {
    this.trustMarkIssuers = Objects.isNull(trustMarkIssuers) ||
        trustMarkIssuers.isEmpty() ? null : String.join(",", trustMarkIssuers);
  }

  /**
   * Sets the critical data by taking a list of strings as input. If the provided list is empty, the critical data is
   * set to null. Otherwise, the strings in the list are joined into a single comma-separated string.
   *
   * @param critical the list of strings to set as critical data
   */
  public void setCritical(final List<String> critical) {
    this.critical = Objects.isNull(critical) || critical.isEmpty() ? null : String.join(",", critical);
  }

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
