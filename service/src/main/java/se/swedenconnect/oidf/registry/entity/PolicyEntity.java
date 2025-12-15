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
package se.swedenconnect.oidf.registry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * PolicyDao is a JPA entity representing a database table for storing policies as JSON objects with the policy id as
 * key.
 *
 * @author David Goldring
 */
@Getter
@Setter
@Entity
@Table(name = "policies")
@FilterDef(name = "fkTypeFilter", parameters = @ParamDef(name = "POLICIES", type = String.class))

public class PolicyEntity extends BaseEntity {
  @Id
  @Column(name = "policy_id", unique = true, updatable = false, nullable = false)
  private UUID policyId;

  @Column(name = "organization_id", insertable = false, updatable = false, nullable = false)
  private UUID organizationId;

  @ManyToOne
  @JoinColumn(name = "organization_id")
  private OrganizationEntity organization;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "policy", nullable = false)
  private String policy;

}


