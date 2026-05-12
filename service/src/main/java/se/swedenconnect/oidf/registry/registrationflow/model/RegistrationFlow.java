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
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import se.swedenconnect.oidf.registry.infrastructure.persistence.JsonConverter;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrationflow.dto.Technology;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

/**
 * Entity class representing the 'subordinate' table in the database. This class represents a Subordinate entity and
 * extends the {@link BaseEntity}, inheriting auditing fields like created date, last modified date, created by, and
 * last modified by.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@ToString(callSuper = true)
@Entity
@NoArgsConstructor
@SuperBuilder
@Table(name = "registration_flow")
public class RegistrationFlow extends BaseEntity {

  @Id
  @Column(name = "flow_id", columnDefinition = "char(36)", nullable = false, updatable = false)
  @JdbcTypeCode(SqlTypes.CHAR)
  private UUID flowId;

  @ManyToOne
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(name = "name", length = 255)
  private String name;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "description_sv", columnDefinition = "TEXT")
  private String descriptionSv;

  @Enumerated(EnumType.STRING)
  @Column(name = "technology", length = 10)
  private Technology technology;

  @Column(name = "entity_type", length = 100)
  private String entityType;

  @Column(name = "flowDefinition", columnDefinition = "TEXT")
  @Convert(converter = RegistrationFlow.StepConverter.class)
  private List<StepModel> flowDefinition;

  /** JPA converter for the flow definition list. */
  @Converter
  public static class StepConverter extends JsonConverter<List<StepModel>> {

    /**
     * Constructor.
     *
     * @param mapper the JSON mapper
     */
    public StepConverter(final JsonMapper mapper) {
      super(mapper, new tools.jackson.core.type.TypeReference<List<StepModel>>() {});
    }
  }

}
