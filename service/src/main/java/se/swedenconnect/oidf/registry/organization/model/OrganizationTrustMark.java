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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.registry.infrastructure.persistence.BaseEntity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * A trust mark type the tenant operator has pre-approved for an {@link Organization}, mapped to the
 * {@code organization_trust_mark} table.
 *
 * <p>These are stored and exposed only — no registration flow behaviour is derived from them yet. The primary
 * key is the {@code (organization, trustMarkType)} pair, so the same trust mark type can be pre-approved for
 * several organizations but never twice for the same one.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Entity
@Table(name = "organization_trust_mark")
@IdClass(OrganizationTrustMark.OrganizationTrustMarkId.class)
public class OrganizationTrustMark extends BaseEntity {

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.DETACH)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Id
  @Column(name = "trust_mark_type", length = 255, nullable = false)
  private String trustMarkType;

  /**
   * Composite primary key of {@link OrganizationTrustMark}. The {@code organization} field carries the
   * identifier type of the associated {@link Organization}, as required for an {@code @Id @ManyToOne} mapping.
   *
   * @author Felix Hellman
   */
  public static class OrganizationTrustMarkId implements Serializable {

    private UUID organization;
    private String trustMarkType;

    /** Default constructor, required by JPA. */
    public OrganizationTrustMarkId() {
    }

    /**
     * Constructor.
     *
     * @param organization the identifier of the organization the trust mark type is pre-approved for
     * @param trustMarkType the pre-approved trust mark type
     */
    public OrganizationTrustMarkId(final UUID organization, final String trustMarkType) {
      this.organization = organization;
      this.trustMarkType = trustMarkType;
    }

    @Override
    public boolean equals(final Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof final OrganizationTrustMarkId that)) {
        return false;
      }
      return Objects.equals(this.organization, that.organization)
          && Objects.equals(this.trustMarkType, that.trustMarkType);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.organization, this.trustMarkType);
    }
  }
}
