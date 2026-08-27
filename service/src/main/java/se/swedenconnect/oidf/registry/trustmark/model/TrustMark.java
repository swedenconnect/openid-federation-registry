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
package se.swedenconnect.oidf.registry.trustmark.model;

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
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;

import java.util.List;
import java.util.UUID;

/**
 * Entity class representing the 'trustmark' table in the database. This class extends {@link BaseEntity}, inheriting
 * common auditing fields such as created date, last modified date, created by, and last modified by. A TrustMark
 * represents a specific trust mark and is associated with a {@link TrustMarkIssuer}. It includes unique
 * identification and
 * relational mapping to the corresponding trustmark issuer.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "trustmark")
public class TrustMark extends BaseEntity implements Persistable<UUID> {

  @Id
  @Column(name = "trustmark_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID trustmarkId;

  /**
   * Tracks whether this instance has been persisted yet, so {@code save()} performs an insert for a freshly constructed
   * trust mark and a proper update for one loaded from the database — {@code trustmarkId} is caller-assignable (not
   * {@code @GeneratedValue}), so Spring Data can't infer this from the ID alone the way it does for generated keys.
   * Without this, a caller-selected ID matching an existing row would silently merge into it.
   */
  @Transient
  @Builder.Default
  private boolean isNew = true;

  @ManyToOne
  @JoinColumn(name = "trustmarkissuer_id", nullable = false, insertable = true, updatable = false)
  private TrustMarkIssuer trustmarkIssuer;

  @OneToMany(mappedBy = "trustMark", cascade = CascadeType.DETACH, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<TrustMarkSubject> trustmarksubjects;

  @Column(name = "trustmark_type")
  private String trustmarkType;

  @Column(name = "logo_uri", length = 512)
  private String logoUri;

  @Column(name = "ref_uri", length = 512)
  private String refUri;

  @Column(name = "delegation", columnDefinition = "TEXT")
  private String delegation;

  @Override
  public UUID getId() {
    return this.trustmarkId;
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
