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
package se.swedenconnect.oidf.registry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import se.swedenconnect.oidf.registry.entity.converters.MapConverter;
import se.swedenconnect.oidf.registry.entity.converters.StringListConverter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * EntityDao is a JPA entity representing a database table for storing entities as JSON objects with the objects Subject
 * value as a key.
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
  @Convert(converter = MapConverter.class)
  private Map<String,Object> metadata;

  @Column(name = "jwks", columnDefinition = "TEXT")
  private String jwks;

  @Column(name = "issuer", columnDefinition = "TEXT")
  private String issuer;

  @Column(name = "subject", columnDefinition = "TEXT")
  private String subject;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @OneToOne(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private TaImEntity trustanchorIntermediate;

  @OneToOne(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private ResolverEntity resolver;

  @OneToOne(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private TrustmarkIssuerEntity trustmarkIssuer;

  @Column(name = "crit")
  @Convert(converter = StringListConverter.class)
  private List<String> crit;

  @Column(name = "ec_location")
  @Convert(converter = StringListConverter.class)
  private List<String> ecLocation;

  @Column(name = "trustmarksources", columnDefinition = "TEXT")
  private String trustmarksources;

  @Column(name = "authorityhints", columnDefinition = "TEXT")
  @Convert(converter = StringListConverter.class)
  private List<String> authorityhints;




  /**
   * Test if this entity has modules
   * @return true it this entity has modules
   */
  public boolean hasModules() {
    if (this.entityType == EntityKeyType.HOSTED_ENTITY) {
      return false;
    }
    return this.trustanchorIntermediate != null || this.resolver != null || this.trustmarkIssuer != null;
  }

}