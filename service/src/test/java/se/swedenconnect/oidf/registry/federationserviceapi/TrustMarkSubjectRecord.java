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

import static org.hibernate.validator.internal.util.Contracts.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Subject of a TrustMark
 *
 * @param sub EntityId Subject
 * @param granted When the subject is granted can be in the future
 * @param expires When the subject expires, if left empty, no expiry date will be used
 * @param revoked If this trust mark is revoked, no new trustmarks for this subject will be issued
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Slf4j
@Builder(toBuilder = true)
public record TrustMarkSubjectRecord(
    String sub,
    @Nullable Instant granted,
    @Nullable Instant expires,
    boolean revoked) implements Serializable {

  /**
   * Converting JSON structure to TrustMarkIssuerSubject object Subject is mandatory and will be checked.
   *
   * @param record Reading subject, expires, granted, revoked values.
   * @return TrustMarkIssuerSubject filled with data
   */
  public static TrustMarkSubjectRecord fromJson(final Map<String, Object> record) {
    final TrustMarkSubjectRecordBuilder tmisBuilder = TrustMarkSubjectRecord.builder()
        .sub((String) record.get("subject"));

    Optional.ofNullable((Boolean) record.get("revoked")).ifPresent(tmisBuilder::revoked);

    Optional.ofNullable((String) record.get("expires"))
        .filter(string -> !string.isBlank())
        .map(Instant::parse)
        .ifPresent(tmisBuilder::expires);

    Optional.ofNullable((String) record.get("granted"))
        .filter(string -> !string.isBlank())
        .map(Instant::parse)
        .ifPresent(tmisBuilder::granted);

    final TrustMarkSubjectRecord tms = tmisBuilder.build();
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
    assertNotEmpty(this.sub, "Subject is expected");
    if (Objects.nonNull(this.granted) && Objects.nonNull(this.expires)) {
      assertTrue(this.granted.isBefore(this.expires), "Expires expected to be after " +
          "granted");
      if (this.expires.isBefore(Instant.now())) {
        log.warn("TrustMark subject:'{}' has already expired. Consider to remove it. Expires:'{}'",
            this.sub, this.expires);
      }
    }
  }

  /**
   * @return this instance to JSON
   */
  public Map<String, Object> toJson() {
    final Map<String, Object> subject = Map.of(
        "sub", this.sub,
        "revoked", this.revoked
    );
    final HashMap<String, Object> json = new HashMap<>(subject);
    Optional.ofNullable(this.expires).ifPresent(exp -> json.put("expires", exp));
    Optional.ofNullable(this.granted).ifPresent(granted -> json.put("granted", granted));
    return json;
  }
}


