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

package se.swedenconnect.oidf.registry.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.registry.api.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.api.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.api.dto.PolicyDto;
import se.swedenconnect.oidf.registry.api.dto.ResolverDto;
import se.swedenconnect.oidf.registry.api.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.api.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;

/**
 * Validator for DTO objects using PropertyValidators framework.
 *
 * @author Per Fredrik Plars
 */
public class ValidateDto {
  private final PropertyValidators.ValidationStringBuilder v;
  private static final ObjectMapper mapper = new ObjectMapper();

  public ValidateDto(final OrganizationRecord organizationRecord) {
    final PropertyValidators propertyValidators = new PropertyValidators();
    this.v = propertyValidators.builder(VariabelValueResolver.orgResolver(organizationRecord));
  }

  public void validate(final FederationEntityDto dto) {
    this.v.required().startsWith("@{entityprefix}").entityid().build()
        .validate("subject", dto.getSubject());

    this.v.required().startsWith("@{entityprefix}").entityid().build()
        .validate("issuer", dto.getIssuer());

    // metadata: json
    if (dto.getMetadata() != null) {
      try {
        final String metadataJson = mapper.writeValueAsString(dto.getMetadata());
        this.v.json().build().validate("metadata", metadataJson);
      }
      catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new PropertyValidationFailException("metadata", dto.getMetadata().toString(),
            "Invalid JSON format: " + e.getMessage());
      }
    }
  }

  public void validate(final HostedEntityDto dto) {
    this.v.required().entityid().build()
        .validate("subject", dto.getSubject());

    this.v.required().entityid().build()
        .validate("issuer", dto.getIssuer());

    // metadata: json
    if (dto.getMetadata() != null) {
      try {
        final String metadataJson = mapper.writeValueAsString(dto.getMetadata());
        this.v.json().build().validate("metadata", metadataJson);
      }
      catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new PropertyValidationFailException("metadata", dto.getMetadata().toString(),
            "Invalid JSON format: " + e.getMessage());
      }
    }
  }

  public void validate(final SubordinateEntityDto dto) {
    this.v.required().entityid().build().validate("subject", dto.getSubject());
    this.v.required().entityid().build().validate("issuer", dto.getIssuer());
    // jwks: required | jwks
    this.v.required().jwks().build().validate("jwks", dto.getJwks());
  }

  public void validate(final ResolverDto dto) {
    // entityId: required | uuid (actually entityid, not uuid)
    this.v.required().entityid().build().validate("entityId", dto.getEntityId());

    // active: required
    this.v.required().build().validate("active", dto.getActive() != null ? dto.getActive().toString() : null);

    // resolveResponseDuration: required | duration
    this.v.required().duration().build().validate("resolveResponseDuration", dto.getResolveResponseDuration());

    // trustAnchor: required | url
    this.v.required().url().build().validate("trustAnchor", dto.getTrustAnchor());

    // trustedKeys: required | jwks
    this.v.required().jwks().build().validate("trustedKeys", dto.getTrustedKeys());

    // stepRetryDuration: required | duration
    this.v.required().duration().build().validate("stepRetryDuration", dto.getStepRetryDuration());
  }

  public void validate(final TrustAnchorDto dto) {
    // entityId: required | uuid (actually entityid, not uuid)
    this.v.required().entityid().build().validate("entityId", dto.getEntityId());

    // active: required
    this.v.required().build().validate("active", dto.getActive() != null ? dto.getActive().toString() : null);

    // trustMarkIssuers: json (list of entity IDs)
    if (dto.getTrustMarkIssuers() != null) {
      try {
        final String trustMarkIssuersJson = mapper.writeValueAsString(dto.getTrustMarkIssuers());
        this.v.json().build().validate("trustMarkIssuers", trustMarkIssuersJson);
      }
      catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new PropertyValidationFailException("trustMarkIssuers", dto.getTrustMarkIssuers().toString(),
            "Invalid JSON format: " + e.getMessage());
      }
    }
  }

  public void validate(final TrustmarkDto dto) {
    // trustmarkissuerId: required | uuid
    this.v.required().uuid().build().validate("trustmarkissuerId", dto.getTrustmarkissuerId());

    // trustMarkEntityId: required | url
    this.v.required().url().build().validate("trustMarkEntityId", dto.getTrustMarkEntityId());

    // logoUri: url
    this.v.url().build().validate("logoUri", dto.getLogoUri());

    // refUri: url
    this.v.url().build().validate("refUri", dto.getRefUri());

    // delegation: jwt:delegation
    this.v.jwt().build().validate("delegation", dto.getDelegation());
  }

  public void validate(final PolicyDto dto) {
    // name: required
    this.v.required().build().validate("name", dto.getName());

    // policy: required | json
    if (dto.getPolicy() != null) {
      try {
        final String policyJson = mapper.writeValueAsString(dto.getPolicy());
        this.v.required().json().build().validate("policy", policyJson);
      }
      catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new PropertyValidationFailException("policy", dto.getPolicy().toString(),
            "Invalid JSON format: " + e.getMessage());
      }
    }
  }

  public void validate(final TrustmarkSubjectDto dto) {
    this.v.required().entityid().build().validate("trustmarkId", dto.getTrustmarkId());
    this.v.required().entityid().build().validate("subject", dto.getSubject());
    this.v.required().build().validate("revoked", dto.getRevoked() != null ? dto.getRevoked().toString() : null);

    // granted and expires are LocalDateTime and will be validated by Jackson during deserialization
    // No additional validation needed here
  }
}

