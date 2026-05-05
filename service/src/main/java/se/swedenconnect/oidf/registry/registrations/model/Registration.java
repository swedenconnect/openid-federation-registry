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
package se.swedenconnect.oidf.registry.registrations.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an incoming registration request in the {@code registrations} table.
 * <p>
 * Automatic flows transition directly to {@link RegistrationStatus#APPROVED} once
 * a subordinate statement is created. Manual flows start as {@link RegistrationStatus#PENDING}
 * until an operator reviews them.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@NoArgsConstructor
@SuperBuilder
@Table(name = "registrations")
public class Registration extends BaseEntity {

  @Id
  @Column(name = "registration_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID registrationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "taim_id", nullable = false)
  private TrustAnchorIntermediateModule taIm;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registration_flow_id", nullable = false)
  private RegistrationFlow registrationFlow;

  @Column(name = "entity_id", length = 255, nullable = false)
  private String entityId;

  @Column(name = "jwks", columnDefinition = "TEXT")
  private String jwks;

  @Column(name = "metadata_policy", columnDefinition = "TEXT")
  private String metadataPolicy;

  @Column(name = "trustmarks_requested", columnDefinition = "TEXT")
  private String trustmarksRequested;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private RegistrationStatus status;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "reviewed_by", length = 255)
  private String reviewedBy;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private RegistrationStatus rejectionReason;
}