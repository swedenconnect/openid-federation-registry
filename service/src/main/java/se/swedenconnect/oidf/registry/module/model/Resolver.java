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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.MapConverter;

import java.util.Map;
import java.util.UUID;

/**
 * Entity class representing the 'resolver' table in the database. This class extends {@link BaseEntity}, inheriting
 * auditing fields like created date, last modified date, created by, and last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "resolver")
public class Resolver extends BaseEntity implements Persistable<UUID> {

  @Id
  @Column(name = "resolver_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID resolverId;

  /**
   * Tracks whether this instance has been persisted yet, so {@code save()} performs an insert for a freshly constructed
   * resolver and a proper update for one loaded from the database — {@code resolverId} is caller-assignable (not
   * {@code @GeneratedValue}), so Spring Data can't infer this from the ID alone the way it does for generated keys.
   * Without this, a caller-selected ID matching an existing row would silently merge into it.
   */
  @Transient
  @Builder.Default
  private boolean isNew = true;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
  @JoinColumn(name = "entity_id", nullable = false)
  private FederationEntity entity;

  @Column(name = "active", nullable = false)
  private Boolean active;

  @Column(name = "resolve_response_duration", nullable = false)
  private String resolveResponseDuration;

  @Column(name = "trust_anchor", nullable = false)
  private String trustAnchor;

  @Column(name = "trusted_keys", columnDefinition = "TEXT", nullable = false)
  @Convert(converter = MapConverter.class)
  private Map<String,Object> trustedKeys;

  @Column(name = "step_cached_value_threshold", nullable = false)
  private Integer stepCachedValueThreshold;

  @Override
  public UUID getId() {
    return this.resolverId;
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
