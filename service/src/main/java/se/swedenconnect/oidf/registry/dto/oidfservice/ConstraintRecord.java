/*
 * Copyright 2024-2025 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.registry.dto.oidfservice;

import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Record for constraints in intermediate/trust anchor.
 *
 * @author Felix Hellman
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintRecord {
  @SerializedName("max-path-length")
  private Long maxPathLength;
  @SerializedName("naming")
  private NamingConstraints naming;
  @SerializedName("allowed-entity-types")
  private List<String> allowedEntityTypes;

  /**
   * @return this object as json
   */
  public Map<String, Object> toJson() {
    final HashMap<String, Object> json = new HashMap<>();

    Optional.ofNullable(this.maxPathLength).ifPresent(length -> {
      json.put("max_path_length", length);
    });

    Optional.ofNullable(this.naming).ifPresent(namingConstraints -> {
      json.put("naming_constraints", namingConstraints.toJson());
    });

    Optional.ofNullable(this.allowedEntityTypes)
        .ifPresent(allowed -> {
          json.put("allowed_entity_types", allowed);
        });

    return json;
  }

  /**
   * @param json to read values from
   * @return new instance
   */
  public static ConstraintRecord fromJson(final Map<String, Object> json) {
    final ConstraintRecordBuilder builder = ConstraintRecord.builder();

    Optional.ofNullable(json.get("max_path_length")).ifPresent(length -> {
      builder.maxPathLength((long) length);
    });

    Optional.ofNullable(json.get("naming_constraints")).ifPresent(namingConstraints -> {
      builder.naming(NamingConstraints.fromJson((Map<String, Object>) namingConstraints));
    });

    Optional.ofNullable(json.get("allowed_entity_types")).ifPresent(allowed -> {
      builder.allowedEntityTypes((List<String>) allowed);
    });

    return builder.build();
  }
}
