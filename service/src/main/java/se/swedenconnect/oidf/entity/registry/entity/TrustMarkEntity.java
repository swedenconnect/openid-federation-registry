/*
 * Copyright 2025 Sweden Connect
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
package se.swedenconnect.oidf.entity.registry.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import se.swedenconnect.oidf.entity.registry.common.BaseEntity;

import java.util.UUID;

/**
 * Entity class representing the 'trustmark' table in the database. This class extends {@link BaseEntity}, inheriting
 * common auditing fields such as created date, last modified date, created by, and last modified by. A TrustMarkEntity
 * represents a specific trust mark and is associated with a {@link ModuleEntity}. It includes unique identification and
 * relational mapping to the corresponding module.
 *
 * @author Per Fredrik Plars
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@ToString(callSuper = true)
@Table(name = "trustmark")
public class TrustMarkEntity extends BaseEntity {

  @Id
  @Column(name = "trustmark_id", nullable = false, updatable = false)
  private UUID trustmarkId;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
  @JoinColumn(name = "module_id", referencedColumnName = "module_id", insertable = true, updatable = false)
  private ModuleEntity module;

}