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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.List;
import java.util.UUID;

/**
 * TrustMarkSubjectEntity is a JPA entity representing a database table for storing entities as JSON objects with the
 * objects Subject value as key.
 *
 * @author Per Fredrik Plars
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Builder(toBuilder = true)
@Table(name = "trustmark_subject")
@FilterDef(name = "fkTypeTMSFilter", parameters = @ParamDef(name = "TRUSTMARKSUBJECT", type = String.class))
public class TrustMarkSubjectEntity extends BaseEntity {

  @Id
  @Column(name = "trustmarksubject_id", nullable = false, updatable = false)
  private UUID trustmarksubjectId;

  /**
   * Constructs a new TrustMarkSubjectEntity with the specified trustmark subject ID, associated TrustMarkEntity,
   * and a list of SettingsEntity instances.
   *
   * @param trustmarksubjectId the unique identifier for the TrustMarkSubjectEntity
   * @param trustMark the TrustMarkEntity instance associated with this TrustMarkSubjectEntity
   * @param settingsEntityList the list of SettingsEntity instances linked to this TrustMarkSubjectEntity
   */
  public TrustMarkSubjectEntity(final UUID trustmarksubjectId, final TrustMarkEntity trustMark,
      final List<SettingsEntity> settingsEntityList) {
    this.trustmarksubjectId = trustmarksubjectId;
    this.trustMark = trustMark;
    this.settingsEntityList = settingsEntityList;
  }

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "trustmark_id", referencedColumnName = "trustmark_id")
  private TrustMarkEntity trustMark;

  /**
   * Retrieves the unique identifier of the associated TrustMarkEntity.
   *
   * @return the UUID representing the ID of the TrustMarkEntity associated with this TrustMarkSubjectEntity
   */
  public UUID getTrustmarkId() {
    return this.trustMark.getTrustmarkId();
  }

  /**
   * Sets the TrustMarkEntity instance associated with this TrustMarkSubjectEntity. Links this TrustMarkSubjectEntity to
   * the specified TrustMarkEntity by adding this entity to the list of subjects within the provided TrustMarkEntity.
   *
   * @param trustMark the TrustMarkEntity instance to be associated with this entity
   */
  public void setTrustMark(final TrustMarkEntity trustMark) {
    this.trustMark = trustMark;
    trustMark.getTrustmarksubjects().add(this);
  }

  @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
  @JoinColumn(name = "fk_id", referencedColumnName = "trustmarksubject_id", insertable = false, updatable = false)
  @Filter(name = "fkTypeTMSFilter", condition = "fk_type = :fkTypeParam")
  private List<SettingsEntity> settingsEntityList;

}