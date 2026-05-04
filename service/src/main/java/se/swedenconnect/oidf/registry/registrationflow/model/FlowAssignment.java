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
package se.swedenconnect.oidf.registry.registrationflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;

import java.util.UUID;

/**
 * Assignment entity linking a registration flow to an intermediate.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registration_flow_assignment",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ta_im_id", "flow_id"}))
public class FlowAssignment extends BaseEntity {

  @Id
  @Column(name = "assign_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID assignId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ta_im_id", nullable = false)
  private TrustAnchorIntermediateModule taIm;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "flow_id", nullable = false)
  private RegistrationFlow registrationFlow;

  /**
   * Creates a new assignment.
   *
   * @param assignId the unique assignment ID
   * @param taIm the intermediate
   * @param registrationFlow the flow to assign
   */
  public FlowAssignment(final UUID assignId, final TrustAnchorIntermediateModule taIm,
      final RegistrationFlow registrationFlow) {
    this.assignId = assignId;
    this.taIm = taIm;
    this.registrationFlow = registrationFlow;
  }
}