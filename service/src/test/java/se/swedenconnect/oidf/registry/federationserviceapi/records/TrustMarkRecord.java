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
import se.swedenconnect.oidf.registry.federationserviceapi.TrustMarkSubjectRecord;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class TrustMarkRecord implements Serializable {
  private final String trustMarkIssuerId;
  private final String trustMarkId;
  private final List<TrustMarkSubjectRecord> subjects;
  private final String logoUri;
  private final String ref;
  private final String delegation;

  public TrustMarkRecord(final String trustMarkIssuerId,
      final String trustMarkId,
      final List<TrustMarkSubjectRecord> subjects,
      final String logoUri,
      final String ref,
      final String delegation) {
    this.trustMarkIssuerId = trustMarkIssuerId;
    this.trustMarkId = trustMarkId;
    this.subjects = subjects;
    this.logoUri = logoUri;
    this.ref = ref;
    this.delegation = delegation;
  }

  public Map<String, Object> toJson() {
    final Map<String, Object> json = new HashMap<>();
    json.put("trust_mark_issuer_id", this.trustMarkIssuerId);
    json.put("trust_mark_id", this.trustMarkId);
    json.put("delegation", this.delegation);
    json.put("ref", this.ref);
    json.put("logo_uri", this.logoUri);
    json.put("subjects", this.subjects.stream().map(TrustMarkSubjectRecord::toJson).toList());
    return json;
  }

  public static TrustMarkRecord fromJson(final Map<String, Object> json) {
    final JsonObject jsonObject = new JsonObject(json);

    return TrustMarkRecord.builder()
        .trustMarkIssuerId(jsonObject.getStringValue("trust_mark_issuer_id"))
        .trustMarkId(jsonObject.getStringValue("trust_mark_id"))
        .delegation(jsonObject.getStringValue("delegation"))
        .ref(jsonObject.getStringValue("ref"))
        .logoUri(jsonObject.getStringValue("logo_uri"))
        .subjects(jsonObject.getObjectMapClaim("subjects")
            .stream()
            .map(m -> (Map<String, Object>) m)
            .map(TrustMarkSubjectRecord::fromJson)
            .toList())
        .build();
  }
}
