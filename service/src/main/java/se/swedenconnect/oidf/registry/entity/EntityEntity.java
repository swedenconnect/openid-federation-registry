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
  private String metadata;

  @Column(name = "jwks", columnDefinition = "TEXT")
  private String jwks;

  @Column(name = "issuer", columnDefinition = "TEXT")
  private String issuer;

  @Column(name = "subject", columnDefinition = "TEXT")
  private String subject;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
  @JoinColumn(name = "organization_id", nullable = false)
  private OrganizationEntity organization;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
  @JoinColumn(name = "policy_id")
  private PolicyEntity policyEntity;

  @OneToOne(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private TaImEntity trustanchorIntermediate;

  @OneToOne(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private ResolverEntity resolver;

  @OneToOne(mappedBy = "entity", cascade = CascadeType.REMOVE)
  private TrustmarkIssuerEntity trustmarkIssuer;

}