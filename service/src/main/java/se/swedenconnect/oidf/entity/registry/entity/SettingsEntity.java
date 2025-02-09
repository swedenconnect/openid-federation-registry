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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import se.swedenconnect.oidf.entity.registry.common.BaseEntity;

import java.time.Duration;

/**
 * Entity class representing the 'Settings' table in the database.
 *
 * @author Per Fredrik Plars
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
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

  @Column(name = "fk_id", nullable = false, updatable = false)
  private String fkId;

  @Column(name = "fk_type", nullable = false, updatable = false)
  private String fkType;

  @Column(name = "data_key", nullable = false)
  private String key;

  @Column(name = "description")
  private String description;

  @Column(name = "validation")
  private String validation;

  @Column(name = "data_type", nullable = false)
  private String valueDataType;

  @Column(name = "data_value")
  private String value;

  /**
   * Casts the value stored in the `value` field to the corresponding data type based on the `valueDataType` field,
   * which specifies the type as "text", "boolean", or "number".
   *
   * @return The casted value as an Object. If the `valueDataType` is "text", the value is returned as a String. If the
   *     `valueDataType` is "boolean", the value is returned as a Boolean. If the `valueDataType` is "number", the value
   *     is returned as a Double.
   * @throws IllegalStateException if the `valueDataType` contains an unexpected or unsupported value.
   */
  public Object castValue() {
    return switch (valueDataType.toUpperCase()) {
      case "TEXT" -> value;
      case "OPTIONS" -> value;
      case "BOOLEAN" -> Boolean.valueOf(value);
      case "NUMERIC" -> Double.valueOf(value);
      case "DURATION" -> Duration.parse(value).toMillis();
      default -> throw new IllegalStateException("Unexpected value: " + value.toLowerCase() +
          " for data type: " + valueDataType);
    };
  }



}