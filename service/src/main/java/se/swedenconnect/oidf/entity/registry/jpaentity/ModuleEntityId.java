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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.util.Objects;

@Getter
@Setter
@Embeddable
public class ModuleEntityId implements java.io.Serializable {
  private static final long serialVersionUID = 2825540062953920472L;
  @NotNull
  @Column(name = "module_id", nullable = false)
  private Long moduleId;

  @NotNull
  @Column(name = "instance_id", nullable = false)
  private Long instanceId;

  @NotNull
  @Column(name = "userdomain_id", nullable = false)
  private Integer userdomainId;

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o))
      return false;
    ModuleEntityId entity = (ModuleEntityId) o;
    return Objects.equals(this.instanceId, entity.instanceId) &&
        Objects.equals(this.userdomainId, entity.userdomainId) &&
        Objects.equals(this.moduleId, entity.moduleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instanceId, userdomainId, moduleId);
  }

}