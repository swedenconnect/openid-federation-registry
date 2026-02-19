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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;

import java.util.List;
import java.util.UUID;

/**
 * Entity class representing the 'trustmark_issuer' table in the database. This class extends {@link BaseEntity},
 * inheriting auditing fields like created date, last modified date, created by, and last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)
@Table(name = "trustmark_issuer")
public class TrustMarkIssuer extends BaseEntity {

  @Id
  @Column(name = "trustmark_issuer_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID trustmarkIssuerId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entity_id", nullable = false)
  private FederationEntity entity;

  @Column(name = "active", nullable = false)
  private Boolean active;

  @Column(name = "trust_mark_token_validity_duration", nullable = false)
  private String trustMarkTokenValidityDuration;

  @OneToMany(mappedBy = "trustmarkIssuer", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<TrustMark> trustmarks;
}
