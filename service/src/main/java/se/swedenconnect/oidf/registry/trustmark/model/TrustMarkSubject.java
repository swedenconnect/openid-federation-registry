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
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.registrations.model.Registration;

import java.util.UUID;

/**
 * TrustMarkSubject is a JPA entity representing a database table for storing entities as JSON objects with the
 * objects Subject value as key.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@SuperBuilder
@Table(name = "trustmark_subject")
public class TrustMarkSubject extends BaseEntity implements Persistable<UUID> {

  @Id
  @Column(name = "trustmarksubject_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID trustmarksubjectId;

  /**
   * Tracks whether this instance has been persisted yet, so {@code save()} performs an insert for a freshly constructed
   * subject and a proper update for one loaded from the database — {@code trustmarksubjectId} is caller-assignable (not
   * {@code @GeneratedValue}), so Spring Data can't infer this from the ID alone the way it does for generated keys.
   * Without this, a caller-selected ID matching an existing row would silently merge into it.
   */
  @Transient
  @Builder.Default
  private boolean isNew = true;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "trustmark_id", referencedColumnName = "trustmark_id")
  private TrustMark trustMark;

  @Column(name = "subject")
  private String subject;

  @Column(name = "revoked")
  private Boolean revoked;

  @Column(name = "granted")
  private java.time.OffsetDateTime granted;

  @Column(name = "expires")
  private java.time.OffsetDateTime expires;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registration_id", nullable = true)
  private Registration registration;

  /**
   * Retrieves the unique identifier of the associated TrustMark.
   *
   * @return the UUID representing the ID of the TrustMark associated with this TrustMarkSubject
   */
  public UUID getTrustmarkId() {
    return this.trustMark.getTrustmarkId();
  }

  /**
   * Sets the TrustMark instance associated with this TrustMarkSubject. Links this TrustMarkSubject to
   * the specified TrustMark by adding this entity to the list of subjects within the provided TrustMark.
   *
   * @param trustMark the TrustMark instance to be associated with this entity
   */
  public void setTrustMark(final TrustMark trustMark) {
    this.trustMark = trustMark;
    trustMark.getTrustmarksubjects().add(this);
  }

  @Override
  public UUID getId() {
    return this.trustmarksubjectId;
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
