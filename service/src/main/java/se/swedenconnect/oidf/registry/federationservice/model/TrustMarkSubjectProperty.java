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
package se.swedenconnect.oidf.registry.federationservice.model;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Subject of an TrustMark
 *
 * @param sub EntityId Subject
 * @param granted When subject is granted can be in the future
 * @param expires When subject expires, if left empty no expiredate will be used
 * @param revoked If this trust mark is revoked, no new trustmarks for this subject will be issued
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Slf4j
@Builder(toBuilder = true)
public record TrustMarkSubjectProperty(
    String sub,
    @Nullable Instant granted,
    @Nullable Instant expires,
    boolean revoked) implements Serializable {

  /**
   * Converting json structure to TrustMarkIssuerSubject object Subject is mandatory and will be checked.
   *
   * @param record Reading subject,expires,granted,revoked values.
   * @return TrustMarkIssuerSubject filled with data
   */
  public static TrustMarkSubjectProperty fromJson(final Map<String, Object> record) {
    final TrustMarkSubjectPropertyBuilder tmisBuilder = TrustMarkSubjectProperty.builder()
        .sub((String) record.get(RecordFields.TrustMarkSubject.SUBJECT));

    Optional.ofNullable((Boolean) record.get(RecordFields.TrustMarkSubject.REVOKED)).ifPresent(tmisBuilder::revoked);

    Optional.ofNullable((String) record.get(RecordFields.TrustMarkSubject.EXPIRES))
        .filter(string -> !string.isBlank())
        .map(Instant::parse)
        .ifPresent(tmisBuilder::expires);

    Optional.ofNullable((String) record.get(RecordFields.TrustMarkSubject.GRANTED))
        .filter(string -> !string.isBlank())
        .map(Instant::parse)
        .ifPresent(tmisBuilder::granted);

    final TrustMarkSubjectProperty tms = tmisBuilder.build();
    tms.validate();
    return tms;
  }

  /**
   * Validate content of configuration.
   *
   * @throws IllegalArgumentException is thrown when configuration is missing
   */
  @PostConstruct
  public void validate() {
    FederationAssert.assertNotEmpty(this.sub, "Subject is expected");
    if (Objects.nonNull(this.granted) && Objects.nonNull(this.expires)) {
      FederationAssert.assertTrue(this.granted.isBefore(this.expires), "Expires expected to be after " +
          RecordFields.TrustMarkSubject.GRANTED);
      if (this.expires.isBefore(Instant.now())) {
        log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'",
            this.sub, this.expires);
      }
    }
  }

  /**
   * @return this instance to json
   */
  public Map<String, Object> toJson() {
    final Map<String, Object> subject = Map.of(
        RecordFields.TrustMarkSubject.SUBJECT, this.sub,
        RecordFields.TrustMarkSubject.REVOKED, this.revoked
    );
    final HashMap<String, Object> json = new HashMap<>(subject);
    Optional.ofNullable(this.expires).ifPresent(exp -> json.put(RecordFields.TrustMarkSubject.EXPIRES, exp));
    Optional.ofNullable(this.granted).ifPresent(granted -> json.put(RecordFields.TrustMarkSubject.GRANTED, granted));
    return json;
  }
}


