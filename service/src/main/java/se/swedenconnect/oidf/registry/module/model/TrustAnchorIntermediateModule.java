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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.JsonConverter;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

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
public class TrustAnchorIntermediateModule extends BaseEntity implements Persistable<UUID> {
  @Id
  @Column(name = "ta_im_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID taImId;

  /**
   * Tracks whether this instance has been persisted yet, so {@code save()} performs an insert for a freshly constructed
   * module and a proper update for one loaded from the database — {@code taImId} is caller-assignable (not
   * {@code @GeneratedValue}), so Spring Data can't infer this from the ID alone the way it does for generated keys.
   * Without this, a caller-selected ID matching an existing row would silently merge into it.
   */
  @Transient
  private boolean isNew = true;

  @NotNull
  @Column(name = "module_type", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
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

  /**
   * The trust mark issuers of a trust anchor, stored as a JSON list. Not used by an intermediate.
   */
  @Column(name = "trust_mark_issuers", columnDefinition = "TEXT")
  @Convert(converter = TrustAnchorIssuerConverter.class)
  private List<TrustAnchorIssuer> trustMarkIssuers = new ArrayList<>();

  /**
   * Gets the trust mark issuers of the module.
   *
   * @return the trust mark issuers, an empty list if there are none
   */
  public List<TrustAnchorIssuer> getTrustMarkIssuers() {
    return this.trustMarkIssuers == null ? List.of() : this.trustMarkIssuers;
  }

  /**
   * The trust mark types of a trust anchor with issuers outside the organization, stored as a JSON list. Not used by
   * an intermediate.
   */
  @Column(name = "external_trust_marks", columnDefinition = "TEXT")
  @Convert(converter = ExternalTrustMarkConverter.class)
  private List<ExternalTrustMark> externalTrustMarks = new ArrayList<>();

  /**
   * Gets the external trust marks of the module.
   *
   * @return the external trust marks, an empty list if there are none
   */
  public List<ExternalTrustMark> getExternalTrustMarks() {
    return this.externalTrustMarks == null ? List.of() : this.externalTrustMarks;
  }

  /** JPA converter for the external trust mark list. */
  @Converter
  public static class ExternalTrustMarkConverter extends JsonConverter<List<ExternalTrustMark>> {

    /**
     * Constructor.
     *
     * @param mapper the JSON mapper
     */
    public ExternalTrustMarkConverter(final JsonMapper mapper) {
      super(mapper, new TypeReference<List<ExternalTrustMark>>() {});
    }
  }

  /** JPA converter for the trust mark issuer list. */
  @Converter
  public static class TrustAnchorIssuerConverter extends JsonConverter<List<TrustAnchorIssuer>> {

    /**
     * Constructor.
     *
     * @param mapper the JSON mapper
     */
    public TrustAnchorIssuerConverter(final JsonMapper mapper) {
      super(mapper, new TypeReference<List<TrustAnchorIssuer>>() {});
    }
  }

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

  @Override
  public UUID getId() {
    return this.taImId;
  }

  @Override
  public boolean isNew() {
    return this.isNew;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }

}
