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

package se.swedenconnect.oidf.entity.registry.jpaentity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import se.swedenconnect.oidf.entity.registry.common.BaseEntity;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Entity
@Table(name = "module")

public class ModuleEntity extends BaseEntity {

  @Id
  @Size(max = 255)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "module_id", nullable = false, updatable = false)
  private String moduleId;

  @Size(max = 100)
  @Column(name = "external_id", unique = true, updatable = false, nullable = false)
  private String externalId;

  @Size(max = 255)
  @NotNull
  @Column(name = "module_type", nullable = false)
  private String moduleType;

  @OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
  @JoinColumns({
      @JoinColumn(name = "fk_type", referencedColumnName = "module_type", insertable = false, updatable = false),
      @JoinColumn(name = "fk_id", referencedColumnName = "module_id", insertable = false, updatable = false)
  })
  private List<SettingsEntity> settingsEntityList;

  public Optional<SettingsEntity> getSettingsEntity(String key) {
    return this.settingsEntityList.stream()
        .filter(s -> s.getKey().equals(key))
        .findFirst();
  }

}