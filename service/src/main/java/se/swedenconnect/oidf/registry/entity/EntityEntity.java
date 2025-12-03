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

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * EntityDao is a JPA entity representing a database table for storing entities as JSON objects with the objects Subject
 * value as key.
 *
 * @author David Goldring
 */
@Getter
@Setter
@Entity
@ToString(callSuper = true)
@Table(name = "entities")
public class EntityEntity extends BaseEntity {
  @Id
  @Column(name = "entity_id", unique = true, updatable = false, nullable = false)
  private UUID entityId;

  @Column(name = "entity_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private EntityKeyType entityType;

  @Column(name = "metadata", columnDefinition = "JSON")
  private String metadata;

  @Column(name = "jwks", columnDefinition = "TEXT")
  private String jwks;

  @Column(name = "issuer", columnDefinition = "TEXT")
  private String issuer;

  @Column(name = "subject", columnDefinition = "TEXT")
  private String subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "policy_id")
  private PolicyEntity policyEntity;

  @OneToMany(mappedBy = "entity")
  private List<ModuleEntity> modules;

  /**
   * Retrieves a {@link ModuleEntity} from the list of modules that matches the given module type.
   *
   * @param fkKeyType the type of the module to search for
   * @return an {@link Optional} containing the matching {@link ModuleEntity} if found, or an empty {@link Optional} if
   *     no match is found
   */
  public Optional<ModuleEntity> getModuleByType(FkKeyType fkKeyType) {
    return this.getModules().stream()
        .filter(moduleEntity -> moduleEntity.getModuleType().equals(fkKeyType.name()))
        .findFirst();
  }
}