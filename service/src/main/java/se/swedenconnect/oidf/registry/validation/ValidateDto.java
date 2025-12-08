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
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.dto.PolicyDto;
import se.swedenconnect.oidf.registry.dto.ResolverDto;
import se.swedenconnect.oidf.registry.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.dto.TrustmarkSubjectDto;

/**
 * Validator for DTO objects using PropertyValidators framework.
 *
 * @author Per Fredrik Plars
 */
public class ValidateDto {
  private final PropertyValidators.ValidationStringBuilder v;
  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Constructor.
   *
   * @param organizationRecord the organization record
   */
  public ValidateDto(final OrganizationRecord organizationRecord) {
    final PropertyValidators propertyValidators = new PropertyValidators();
    this.v = propertyValidators.builder(VariabelValueResolver.orgResolver(organizationRecord));
  }

  /**
   * Validates FederationEntityDto.
   *
   * @param dto the federation entity DTO
   */
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

  /**
   * Validates HostedEntityDto.
   *
   * @param dto the hosted entity DTO
   */
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

  /**
   * Validates SubordinateEntityDto.
   *
   * @param dto the subordinate entity DTO
   */
  public void validate(final SubordinateEntityDto dto) {
    this.v.required().entityid().build().validate("subject", dto.getSubject());
    this.v.required().entityid().build().validate("issuer", dto.getIssuer());
    this.v.required().jwks().build().validate("jwks", dto.getJwks());
  }

  /**
   * Validates ResolverDto.
   *
   * @param dto the resolver DTO
   */
  public void validate(final ResolverDto dto) {
    this.v.required().entityid().build().ifFailThrow("entityId", dto.getEntityId());
    this.v.required().build().ifFailThrow("active", dto.getActive());
    this.v.required().duration().build().validate("resolveResponseDuration", dto.getResolveResponseDuration());
    this.v.required().entityid().build().validate("trustAnchor", dto.getTrustAnchor());
    this.v.required().required().jwks().build().validate("trustedKeys", dto.getTrustedKeys());
    this.v.required().duration().build().validate("stepRetryDuration", dto.getStepRetryDuration());
  }

  /**
   * Validates TrustAnchorDto.
   *
   * @param dto the trust anchor DTO
   */
  public void validate(final TrustAnchorDto dto) {
    this.v.required().uuid().build().ifFailThrow("entityId", dto.getEntityId());
    this.v.required().build().ifFailThrow("active", dto.getActive());
    this.v.entityid().build().ifFailThrow("trustmarkissuers", dto.getTrustMarkIssuers());

  }

  /**
   * Validates TrustmarkDto.
   *
   * @param dto the trustmark DTO
   */
  public void validate(final TrustmarkDto dto) {
    this.v.required().uuid().build().validate("trustmarkissuerId", dto.getTrustmarkissuerId());
    this.v.required().url().build().validate("trustMarkEntityId", dto.getTrustMarkEntityId());
    this.v.url().build().validate("logoUri", dto.getLogoUri());
    this.v.url().build().validate("refUri", dto.getRefUri());
    this.v.jwt().build().validate("delegation", dto.getDelegation());
  }

  /**
   * Validates PolicyDto.
   *
   * @param dto the policy DTO
   */
  public void validate(final PolicyDto dto) {
    this.v.required().build().validate("name", dto.getName());
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

  /**
   * Validates TrustmarkSubjectDto.
   *
   * @param dto the trustmark subject DTO
   */
  public void validate(final TrustmarkSubjectDto dto) {
    this.v.required().entityid().build().validate("trustmarkId", dto.getTrustmarkId());
    this.v.required().entityid().build().validate("subject", dto.getSubject());
    this.v.required().build().ifFailThrow("revoked", dto.getRevoked());

    // granted and expires are LocalDateTime and will be validated by Jackson during deserialization
    // No additional validation needed here
  }
}

