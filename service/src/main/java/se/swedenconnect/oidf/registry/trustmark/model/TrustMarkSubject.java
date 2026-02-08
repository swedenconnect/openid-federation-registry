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
package se.swedenconnect.oidf.registry.trustmark.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;

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
public class TrustMarkSubject extends BaseEntity {

  @Id
  @Column(name = "trustmarksubject_id", nullable = false, updatable = false)
  private UUID trustmarksubjectId;

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

}
