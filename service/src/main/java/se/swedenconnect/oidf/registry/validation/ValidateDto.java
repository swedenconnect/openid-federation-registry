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

package se.swedenconnect.oidf.registry.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.*;

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
  private ValidateDto(final OrganizationRecord organizationRecord) {
    final PropertyValidators propertyValidators = new PropertyValidators();
    this.v = propertyValidators.builder(VariableValueResolver.orgResolver(organizationRecord));
  }

  /**
   * Creates a new ValidateDto instance.
   *
   * @param organizationRecord the organization record
   * @return a new ValidateDto instance
   */
  public static ValidateDto init(final OrganizationRecord organizationRecord) {
    return new ValidateDto(organizationRecord);
  }

  /**
   * Validates FederationEntityDto.
   *
   * @param dto the federation entity DTO
   */
  public void validate(final FederationEntityDto dto) {
    this.v.length(1, 500).build().ifFailThrow("crit", dto.getCrit());
  }

  /**
   * Validates HostedEntityDto.
   *
   * @param dto the hosted entity DTO
   */
  public void validate(final HostedEntityDto dto) {
    this.v.required().entityid().build().ifFailThrow("entityidentifier", dto.getEntityIdentifier());
    this.v.required().json().build().ifFailThrow("metadata", dto.getMetadata());
  }

  /**
   * Validates SubordinateEntityDto.
   *
   * @param dto the subordinate entity DTO
   */
  @Deprecated
  public void validate(final SubordinateEntityDto dto) {
    this.v.required().entityid().build().ifFailThrow("subject", dto.getSubject());
    this.v.required().entityid().build().ifFailThrow("issuer", dto.getIssuer());
    this.v.required().jwks().build().ifFailThrow("jwks", dto.getJwks());
    this.v.length(2, 150).build().ifFailThrow("crit", dto.getCrit());
  }

  /**
   * Validates ResolverDto.
   *
   * @param dto the resolver DTO
   */
  public void validate(final ResolverDto dto) {
    this.v.required().uuid().build().ifFailThrow("entityId", dto.getEntityId());
    this.v.required().build().ifFailThrow("active", dto.getActive());
    this.v.required().duration().build().ifFailThrow("resolveResponseDuration", dto.getResolveResponseDuration());
    this.v.required().entityid().build().ifFailThrow("trustAnchor", dto.getTrustAnchor());
    this.v.required().required().jwks().build().ifFailThrow("trustedKeys", dto.getTrustedKeys());
    this.v.required().required().duration().build().ifFailThrow("stepRetryDuration", dto.getStepRetryDuration());
    this.v.required().required().build().ifFailThrow("stepCachedValueThreshold", dto.getStepCachedValueThreshold());
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
   * Validates IntermediateDto.
   *
   * @param dto the intermediate DTO
   */
  public void validate(final IntermediateDto dto) {
    this.v.required().uuid().build().ifFailThrow("entityId", dto.getEntityId());
    this.v.required().build().ifFailThrow("active", dto.getActive());
  }

  /**
   * Validates TrustmarkDto.
   *
   * @param dto the trustmark DTO
   */
  public void validate(final TrustmarkDto dto) {
    this.v.required().uuid().build().ifFailThrow("trustmarkissuerId", dto.getTrustmarkissuerId());
    this.v.required().url().build().ifFailThrow("trustMarkEntityId", dto.getTrustmarkType());
    this.v.url().build().ifFailThrow("logoUri", dto.getLogoUri());
    this.v.url().build().ifFailThrow("refUri", dto.getRefUri());
    this.v.jwt().build().ifFailThrow("delegation", dto.getDelegation());
  }

  /**
   * Validates PolicyDto.
   *
   * @param dto the policy DTO
   */
  public void validate(final PolicyDto dto) {
    this.v.required().build().ifFailThrow("name", dto.getName());
    this.v.required().build().ifFailThrow("policy", dto.getPolicy());

    if (dto.getPolicy() != null) {
      try {
        final String policyJson = mapper.writeValueAsString(dto.getPolicy());
        this.v.required().json().build().ifFailThrow("policy", policyJson);
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
    this.v.required().uuid().build().ifFailThrow("trustmarkId", dto.getTrustmarkId());
    this.v.required().entityid().build().ifFailThrow("subject", dto.getSubject());
    this.v.required().build().ifFailThrow("revoked", dto.getRevoked());
  }

  /**
   * Validates TrustmarkIssuerDto.
   *
   * @param dto the trustmark issuer DTO
   */
  public void validate(final TrustmarkIssuerDto dto) {
    this.v.required().uuid().build().ifFailThrow("entityId", dto.getEntityId());
    this.v.required().build().ifFailThrow("active", dto.getActive());
    this.v.required().duration().build().ifFailThrow("trustMarkTokenValidityDuration",
        dto.getTrustMarkTokenValidityDuration());
  }

  /**
   * Validates SubordinateDto.
   *
   * @param dto the subordinate DTO
   */
  public void validate(final SubordinateDto dto) {
    this.v.required().uuid().build().ifFailThrow("taImId", dto.getTaImId());
    this.v.required().entityid().build().ifFailThrow("entityidentifier", dto.getEntityIdentifier());
    this.v.jwks().build().ifFailThrow("jwks", dto.getJwks());

    this.v.uuid().build().ifFailThrow("policyid", dto.getPolicyId());
    this.v.url().build().ifFailThrow("eclocation", dto.getEcLocation());
    this.v.length(2, 150).build().ifFailThrow("metadatapolicycrit", dto.getMetadataPolicyCrit());
    this.v.length(2, 150).build().ifFailThrow("crit", dto.getCrit());

  }
}

