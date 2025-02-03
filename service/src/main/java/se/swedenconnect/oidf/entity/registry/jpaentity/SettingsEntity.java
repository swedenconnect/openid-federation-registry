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

package se.swedenconnect.oidf.entity.registry.jpaentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import se.swedenconnect.oidf.entity.registry.common.BaseEntity;

/**
 * Entity class representing the 'Settings' table in the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "Settings")
public class SettingsEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "property_id", nullable = false, updatable = false)
  private Long propertyId;

  @Column(name = "fk_id", nullable = false)
  private String fkId;

  @Column(name = "fk_type", nullable = false)
  private String fkType;

  @Column(name = "data_key", nullable = false)
  private String key;

  @Column(name = "description")
  private String description;

  @Column(name = "validation")
  private String validation;

  @Column(name = "data_type")
  private String valueDataType;

  @Column(name = "data_value")
  private String value;

}