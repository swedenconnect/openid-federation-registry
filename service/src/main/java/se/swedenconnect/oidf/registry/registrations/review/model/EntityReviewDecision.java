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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One decision taken in a review session. The decided object (domain or registration) records the outcome itself;
 * this row records that it was taken in this review, and keeps deferrals, which change nothing else.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "entity_review_decision")
public class EntityReviewDecision extends BaseEntity {

  @Id
  @Column(name = "decision_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID decisionId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.DETACH)
  @JoinColumn(name = "review_id", nullable = false)
  private EntityReview review;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", length = 20, nullable = false)
  private ReviewItemType itemType;

  @Column(name = "item_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID itemId;

  @Column(name = "step_index")
  private Integer stepIndex;

  @Column(name = "step_name", length = 255)
  private String stepName;

  @Enumerated(EnumType.STRING)
  @Column(name = "decision", length = 20, nullable = false)
  private ReviewDecision decision;

  @Column(name = "reason", columnDefinition = "TEXT")
  private String reason;

  @Column(name = "decided_by", length = 255, nullable = false)
  private String decidedBy;

  @Column(name = "decided_at", nullable = false)
  private LocalDateTime decidedAt;
}
