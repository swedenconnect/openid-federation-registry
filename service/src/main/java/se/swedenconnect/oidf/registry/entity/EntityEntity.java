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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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

  @Column(name = "crit")
  private String crit;

  @Column(name = "metadataPolicyCrit")
  private String metadataPolicyCrit;

  /**
   * Sets the critical data by taking a list of strings as input. If the provided list is empty, the critical data is
   * set to null. Otherwise, the strings in the list are joined into a single comma-separated string.
   *
   * @param crit the list of strings to set as critical data
   */
  public void setCrit(final List<String> crit) {
    this.crit = Objects.isNull(crit) || crit.isEmpty() ? null : String.join(",", crit);
  }

  /**
   * Sets the critical data by taking a list of strings as input. If the provided list is empty, the critical data is
   * set to null. Otherwise, the strings in the list are joined into a single comma-separated string.
   *
   * @param metadataPolicyCrit the list of strings to set as critical data
   */
  public void setMetadataPolicyCrit(final List<String> metadataPolicyCrit) {
    this.metadataPolicyCrit =
        Objects.isNull(metadataPolicyCrit) ||
            metadataPolicyCrit.isEmpty() ? null : String.join(",", metadataPolicyCrit);
  }

  /**
   * Retrieves a list of critical components by splitting the stored string using a comma as a delimiter. If the stored
   * string is empty, returns null.
   *
   * @return a list of critical component strings, or null if no critical components are available
   */
  public List<String> getCrit() {
    if (this.crit == null) {
      return Collections.emptyList();
    }
    return this.crit.isEmpty() ? null : List.of(this.crit.split(","));
  }

  /**
   * Retrieves a list of critical components by splitting the stored string using a comma as a delimiter. If the stored
   * string is empty, returns null.
   *
   * @return a list of critical component strings, or null if no critical components are available
   */
  public List<String> getMetadataPolicyCrit() {
    if (this.metadataPolicyCrit == null) {
      return Collections.emptyList();
    }
    return this.metadataPolicyCrit.isEmpty() ? null : List.of(this.crit.split(","));
  }

}