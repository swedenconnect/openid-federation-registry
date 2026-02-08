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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;

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
public class Resolver extends BaseEntity {

  @Id
  @Column(name = "resolver_id", nullable = false, updatable = false)
  private UUID resolverId;

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
  private String trustedKeys;

  @Column(name = "step_retry_duration", nullable = false)
  private String stepRetryDuration;

  @Column(name = "step_cached_value_threshold", nullable = false)
  private Integer stepCachedValueThreshold;
}
