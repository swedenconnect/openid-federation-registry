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
package se.swedenconnect.oidf.registry.federationserviceapi.records;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data class for hosted record.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
@Builder
public class HostedRecord implements Serializable {
  private final Map<String, Object> metadata;
  private final List<TrustMarkSource> trustMarkSources;
  private final List<String> authorityHints;

  /**
   * Constructor.
   *
   * @param metadata for this record
   * @param trustMarkSources for this record
   * @param authorityHints for this record
   */
  public HostedRecord(
      final Map<String, Object> metadata,
      final List<TrustMarkSource> trustMarkSources,
      final List<String> authorityHints) {

    this.metadata = metadata;
    this.trustMarkSources = trustMarkSources;
    this.authorityHints = authorityHints;
  }

  /**
   * @param json to create an instance from
   * @return new instance
   */
  public static HostedRecord fromJson(final Map<String, Object> json) {
    if (json == null) {
      return null;
    }
    final Map<String, Object> map = (Map<String, Object>) json.get("metadata");
    final List<Map<String, Object>> tms = (List<Map<String, Object>>) json.get("trust_mark_sources");
    final List<String> authorityHints = (List<String>) json.get("authority_hints");
    return new HostedRecord(
        Optional.ofNullable(map).orElseGet(Map::of),
        Optional.ofNullable(tms).map(trustmarksource -> trustmarksource
            .stream()
            .map(TrustMarkSource::fromJson)
            .toList()
        ).orElseGet(List::of),
        Optional.ofNullable(authorityHints).orElseGet(List::of)
    );
  }

  /**
   * @return this instance converted to JSON (map) structure for nimbus
   */
  public Map<String, Object> toJson() {
    return Map.of(
        "authority_hints", Optional.ofNullable(this.authorityHints).orElseGet(List::of),
        "metadata", this.metadata,
        "trust_mark_sources", Optional.ofNullable(this.trustMarkSources)
            .map(tms -> tms.stream()
                .map(TrustMarkSource::toJson)
                .toList()).orElseGet(List::of)
    );
  }
}
