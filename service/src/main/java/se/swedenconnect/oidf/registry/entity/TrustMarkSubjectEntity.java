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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.List;
import java.util.Optional;
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
@SuperBuilder
@Table(name = "trustmark_subject")
@FilterDef(name = "fkTypeTMSFilter", parameters = @ParamDef(name = "TRUSTMARKSUBJECT", type = String.class))
public class TrustMarkSubjectEntity extends BaseEntity {

  @Id
  @Column(name = "trustmarksubject_id", nullable = false, updatable = false)
  private UUID trustmarksubjectId;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "trustmark_id", referencedColumnName = "trustmark_id")
  private TrustMarkEntity trustMark;

  @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
  @JoinColumn(name = "fk_id", referencedColumnName = "trustmarksubject_id", insertable = false, updatable = false)
  @Filter(name = "fkTypeTMSFilter", condition = "fk_type = :fkTypeParam")
  private List<SettingsEntity> settingsEntityList;

  @Column(name = "jsondata")
  private String jsondata;



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

  /**
   * Retrieves the {@link SettingsEntity} associated with the specified key. The method searches through the list of
   * settings entities and returns an optional containing the first entity matching the given key, if one exists.
   *
   * @param key the key to search for in the list of settings entities
   * @return an {@link Optional} containing the matching {@link SettingsEntity} if found, otherwise an empty
   *     {@link Optional}
   */
  public Optional<SettingsEntity> getSettingsEntity(final String key) {
    return this.settingsEntityList.stream()
        .filter(s -> s.getKey().equals(key))
        .findFirst();
  }

}