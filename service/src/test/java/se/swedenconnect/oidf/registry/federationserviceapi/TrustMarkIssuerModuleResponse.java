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
package se.swedenconnect.oidf.registry.federationserviceapi;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TrustMarkIssuer module from registry.
 *
 * @author Felix Hellman
 */
@Getter
@ToString
@Builder
public class TrustMarkIssuerModuleResponse {
  private Duration trustMarkTokenValidityDuration;
  private String entityIdentifier;

  private String alias;
  private Boolean active;
  private List<TrustMarkResponse> trustMarks;

  public static TrustMarkIssuerModuleResponse fromJson(final Map<String, Object> json) {
    return TrustMarkIssuerModuleResponse.builder()
        .trustMarkTokenValidityDuration(Duration.parse((String) json.get("trust_mark_token_validity_duration")))
        .alias((String) json.get("alias"))
        .entityIdentifier((String) json.get("entity_identifier"))

        .active((Boolean) json.get("active"))
        .build();
  }

  public void validate() {
    Assert.notNull(entityIdentifier, "entityIdentifier");
    Assert.notNull(active, "active");
    Assert.notNull(trustMarkTokenValidityDuration, "trustMarkTokenValidityDuration");
    //Assert.isTrue(!trustMarks.isEmpty(), "trustMarks must not be empty");
    //    trustMarks.forEach(TrustMarkResponse::validate);
  }

  @Builder
  public record TrustMarkResponse(String trustMarkId,
      Optional<String> logoUri,
      Optional<String> refUri,
      Optional<String> delegation,
      List<TrustMarkSubjectRecord> trustMarkSubjectRecords) {

    public static TrustMarkResponse fromJson(final Map<String, Object> json) {
      final List<TrustMarkSubjectRecord> trustMarkSubjects =
          Optional.ofNullable((List<Map<String, Object>>) json.get("trust-mark-subjects"))
              .map(claim -> claim.stream().map(TrustMarkSubjectRecord::fromJson).toList()).orElse(List.of());
      return TrustMarkResponse.builder()
          .trustMarkId(((String) json.get("trust_mark_entity_id")))
          .logoUri(Optional.ofNullable((String) json.get("logo_uri")))
          .refUri(Optional.ofNullable((String) json.get("ref_uri")))
          .delegation(Optional.ofNullable((String) json.get("delegation")).map(String::new))
          .trustMarkSubjectRecords(trustMarkSubjects)
          .build();
    }

    public void validate() {
      Assert.notNull(trustMarkId, "trustMarkId");
      Assert.notNull(logoUri, "alias");
      Assert.notNull(refUri, "refUri");
      Assert.notNull(delegation, "delegation");
      Assert.notNull(trustMarkSubjectRecords, "trustMarkSubjectRecords");
      trustMarkSubjectRecords.forEach(TrustMarkSubjectRecord::validate);
    }

  }

}
