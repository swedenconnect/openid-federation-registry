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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
public class SubordinateEntity extends BaseEntity {

  @Id
  @Column(name = "subordinate_id", nullable = false, updatable = false)
  private UUID subordinateId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ta_im_id", nullable = false)
  private TaImEntity taIm;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "policy_id")
  private PolicyEntity policy;

  @Column(name = "jwks", columnDefinition = "TEXT")
  private String jwks;

  @Column(name = "entityidentifyer", length = 255)
  private String entityidentifyer;

  @Column(name = "crit", columnDefinition = "TEXT")
  private String crit;

  @Column(name = "metadata_policy_crit", columnDefinition = "TEXT")
  private String metadataPolicyCrit;

  @Column(name = "ec_location", length = 255)
  private String ecLocation;

  @Column(name = "ec_location_automatic", nullable = false)
  private boolean ecLocationAutomatic;

}

