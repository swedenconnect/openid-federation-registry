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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.JsonConverter;
import se.swedenconnect.oidf.registry.infrastructure.persistence.MapConverter;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrations.dto.StepExecutionRecordDto;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an incoming registration request in the {@code registrations} table.
 * <p>
 * Automatic flows transition directly to {@link RegistrationStatus#APPROVED} once
 * a subordinate statement is created. Manual flows start as {@link RegistrationStatus#PENDING_APPROVAL}
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
  @JoinColumn(name = "assign_id", nullable = false)
  private FlowAssignment flowAssignment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.DETACH)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(name = "entity_id", length = 255, nullable = false)
  private String entityId;

  @Column(name = "jwks", columnDefinition = "TEXT")
  @Convert(converter = MapConverter.class)
  private Map<String,Object> jwks;

  @Column(name = "metadata_policy", columnDefinition = "TEXT")
  @Convert(converter = MapConverter.class)
  private Map<String,Object> metadataPolicy;

  @Column(name = "trustmarks_requested", columnDefinition = "TEXT")
  @Convert(converter = TrustmarkSourceConverter.class)
  private List<TrustmarkSource> trustmarksRequested;

  @Enumerated(EnumType.STRING)
  @Column(name = "registration_type", length = 30, nullable = false)
  private RegistrationType registrationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private RegistrationStatus status;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @Column(name = "reviewed_by", length = 255)
  private String reviewedBy;

  @Column(name = "rejection_reason", columnDefinition = "TEXT")
  private String rejectionReason;

  @Column(name = "step_results", columnDefinition = "TEXT")
  @Convert(converter = Registration.StepResultsConverter.class)
  private List<StepExecutionRecordDto> stepResults;

  @Column(name = "pending_step_index")
  private Integer pendingStepIndex;

  /** JPA converter for the trustmark source list. */
  @Converter
  public static class TrustmarkSourceConverter extends JsonConverter<List<TrustmarkSource>> {

    /**
     * Constructor.
     *
     * @param mapper the JSON mapper
     */
    public TrustmarkSourceConverter(final JsonMapper mapper) {
      super(mapper, new tools.jackson.core.type.TypeReference<List<TrustmarkSource>>() {});
    }
  }

  /** JPA converter for the step results list. */
  @Converter
  public static class StepResultsConverter extends JsonConverter<List<StepExecutionRecordDto>> {

    /**
     * Constructor.
     *
     * @param mapper the JSON mapper
     */
    public StepResultsConverter(final JsonMapper mapper) {
      super(mapper, new tools.jackson.core.type.TypeReference<List<StepExecutionRecordDto>>() {});
    }
  }
}