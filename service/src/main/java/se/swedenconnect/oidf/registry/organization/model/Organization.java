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

package se.swedenconnect.oidf.registry.organization.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;

import java.util.List;
import java.util.UUID;

/**
 * The Organization class represents an organization in the system. This class maps to the "organization" table in
 * the database. It extends the BaseEntity class, inheriting audit fields for created/modified dates and other user
 * metadata.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@Table(name = "organization")
public class Organization extends BaseEntity {
  @Id
  @Column(name = "organization_id", columnDefinition = "char(36)", nullable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID organizationId;

  @Size(max = 255)
  @Column(name = "org_number")
  private String orgNumber;

  @Size(max = 255)
  @Column(name = "org_name")
  private String orgName;

  @Size(max = 255)
  @Column(name = "entityPrefix")
  private String entityPrefix;

  @ManyToOne
  @JoinColumn(name = "instance_id", nullable = false)
  private Instance instance;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.DETACH)
  private List<FederationEntity> entities;

}
