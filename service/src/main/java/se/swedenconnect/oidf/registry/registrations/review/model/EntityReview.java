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
package se.swedenconnect.oidf.registry.registrations.review.model;

import jakarta.persistence.CascadeType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.registrations.model.Registration;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A review session for one entity, identified by the entity's own registration. Started by an operator and continued
 * by any operator of the tenant until nothing is left to decide.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "entity_review")
public class EntityReview extends BaseEntity {

  @Id
  @Column(name = "review_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID reviewId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.DETACH)
  @JoinColumn(name = "registration_id", nullable = false)
  private Registration registration;

  @Column(name = "instance_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID instanceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private EntityReviewStatus status;

  @Column(name = "started_by", length = 255, nullable = false)
  private String startedBy;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;
}
