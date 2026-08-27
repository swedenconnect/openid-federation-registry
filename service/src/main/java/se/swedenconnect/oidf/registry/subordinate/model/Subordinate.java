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

package se.swedenconnect.oidf.registry.subordinate.model;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.MapConverter;
import se.swedenconnect.oidf.registry.infrastructure.persistence.StringListConverter;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity class representing the 'subordinate' table in the database. This class represents a Subordinate entity and
 * extends the {@link BaseEntity}, inheriting auditing fields like created date, last modified date, created by, and
 * last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@NoArgsConstructor
@SuperBuilder
@Table(name = "subordinate")
public class Subordinate extends BaseEntity implements Persistable<UUID> {

  @Id
  @Column(name = "subordinate_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID subordinateId;

  /**
   * Tracks whether this instance has been persisted yet, so {@code save()} performs an insert for a freshly constructed
   * subordinate and a proper update for one loaded from the database — {@code subordinateId} is caller-assignable (not
   * {@code @GeneratedValue}), so Spring Data can't infer this from the ID alone the way it does for generated keys.
   * Without this, a caller-selected ID matching an existing row would silently merge into it.
   */
  @Transient
  @Builder.Default
  private boolean isNew = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ta_im_id", nullable = false)
  private TrustAnchorIntermediateModule taIm;

  @Column(name = "jwks", columnDefinition = "TEXT")
  @Convert(converter = MapConverter.class)
  private Map<String, Object> jwks;

  @Column(name = "entityidentifier", length = 255)
  private String entityidentifier;

  @Column(name = "crit", columnDefinition = "TEXT")
  @Convert(converter = StringListConverter.class)
  private List<String> crit;

  @Column(name = "metadata_policy_crit", columnDefinition = "TEXT")
  @Convert(converter = StringListConverter.class)
  private List<String> metadataPolicyCrit;

  @Column(name = "ec_location", length = 255)
  private String ecLocation;

  @Column(name = "ec_location_automatic", nullable = false)
  private boolean ecLocationAutomatic;

  @Column(name = "metadata_policy", columnDefinition = "TEXT")
  @Convert(converter = MapConverter.class)
  private Map<String, Object> metadataPolicy;

  @Override
  public UUID getId() {
    return this.subordinateId;
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
