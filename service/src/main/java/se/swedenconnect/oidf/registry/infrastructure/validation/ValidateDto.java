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

package se.swedenconnect.oidf.registry.infrastructure.validation;


import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.policy.dto.PolicyDto;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;
import se.swedenconnect.oidf.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.validation.VariableValueResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

/**
 * Validator for DTO objects using PropertyValidators framework.
 *
 * @author Per Fredrik Plars
 */
public class ValidateDto {
  private static final JsonMapper MAPPER = new JsonMapper();
  private static final int MIN_CRIT_LENGTH = 1;
  private static final int MAX_CRIT_LENGTH = 500;
  private static final int MIN_POLICY_CRIT_LENGTH = 2;
  private static final int MAX_POLICY_CRIT_LENGTH = 150;
  private static final String ENTITY_PREFIX = "@{entityprefix}";

  private final PropertyValidators.ValidationBuilder v;

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
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final FederationEntityDto dto) {
    Objects.requireNonNull(dto, "FederationEntityDto cannot be null");

    this.v.required()
        .startsWith(ENTITY_PREFIX)
        .entityid()
        .build()
        .ifFailThrow("entityIdentifier", dto.getEntityIdentifier());

    this.v.length(MIN_CRIT_LENGTH, MAX_CRIT_LENGTH)
        .build()
        .ifFailThrow("crit", dto.getCrit());

    this.v.url()
        .build()
        .ifFailThrow("authorityHints", dto.getAuthorityhints());
  }

  /**
   * Validates HostedEntityDto.
   *
   * @param dto the hosted entity DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final HostedEntityDto dto) {
    Objects.requireNonNull(dto, "HostedEntityDto cannot be null");

    this.v.required()
        .entityid()
        .build()
        .ifFailThrow("entityIdentifier", dto.getEntityIdentifier());

    this.v.required()
        .json()
        .build()
        .ifFailThrow("metadata", dto.getMetadata());

    this.v.length(MIN_CRIT_LENGTH, MAX_CRIT_LENGTH)
        .build()
        .ifFailThrow("crit", dto.getCrit());

    this.v.url()
        .build()
        .ifFailThrow("authorityHints", dto.getAuthorityhints());
  }

  /**
   * Validates ResolverDto.
   *
   * @param dto the resolver DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final ResolverDto dto) {
    Objects.requireNonNull(dto, "ResolverDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("entityId", dto.getEntityId());

    this.v.required()
        .build()
        .ifFailThrow("active", dto.getActive());

    this.v.required()
        .duration()
        .build()
        .ifFailThrow("resolveResponseDuration", dto.getResolveResponseDuration());

    this.v.required()
        .entityid()
        .build()
        .ifFailThrow("trustAnchor", dto.getTrustAnchor());

    this.v.required()
        .jwks()
        .build()
        .ifFailThrow("trustedKeys", dto.getTrustedKeys());

    this.v.required()
        .duration()
        .build()
        .ifFailThrow("stepRetryDuration", dto.getStepRetryDuration());

    this.v.required()
        .build()
        .ifFailThrow("stepCachedValueThreshold", dto.getStepCachedValueThreshold());
  }

  /**
   * Validates TrustAnchorDto.
   *
   * @param dto the trust anchor DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final TrustAnchorDto dto) {
    Objects.requireNonNull(dto, "TrustAnchorDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("entityId", dto.getEntityId());

    this.v.required()
        .build()
        .ifFailThrow("active", dto.getActive());

    this.v.entityid()
        .build()
        .ifFailThrow("trustMarkIssuers", dto.getTrustMarkIssuers());
  }

  /**
   * Validates IntermediateDto.
   *
   * @param dto the intermediate DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final IntermediateDto dto) {
    Objects.requireNonNull(dto, "IntermediateDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("entityId", dto.getEntityId());

    this.v.required()
        .build()
        .ifFailThrow("active", dto.getActive());
  }

  /**
   * Validates TrustmarkDto.
   *
   * @param dto the trustmark DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final TrustmarkDto dto) {
    Objects.requireNonNull(dto, "TrustmarkDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("trustmarkIssuerId", dto.getTrustmarkissuerId());

    this.v.required()
        .url()
        .build()
        .ifFailThrow("trustmarkType", dto.getTrustmarkType());

    this.v.url()
        .build()
        .ifFailThrow("logoUri", dto.getLogoUri());

    this.v.url()
        .build()
        .ifFailThrow("refUri", dto.getRefUri());

    this.v.jwt()
        .build()
        .ifFailThrow("delegation", dto.getDelegation());
  }

  /**
   * Validates PolicyDto.
   *
   * @param dto the policy DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final PolicyDto dto) {
    Objects.requireNonNull(dto, "PolicyDto cannot be null");

    this.v.required()
        .build()
        .ifFailThrow("name", dto.getName());

    this.v.required()
        .oidfPolicy()
        .build()
        .ifFailThrow("policy", dto.getPolicy());

    if (dto.getPolicy() != null) {
      this.validatePolicyJson(dto.getPolicy());
    }
  }

  /**
   * Validates TrustmarkSubjectDto.
   *
   * @param dto the trustmark subject DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final TrustmarkSubjectDto dto) {
    Objects.requireNonNull(dto, "TrustmarkSubjectDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("trustmarkId", dto.getTrustmarkId());

    this.v.required()
        .entityid()
        .build()
        .ifFailThrow("subject", dto.getSubject());

    this.v.required()
        .build()
        .ifFailThrow("revoked", dto.getRevoked());
  }

  /**
   * Validates TrustmarkIssuerDto.
   *
   * @param dto the trustmark issuer DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final TrustmarkIssuerDto dto) {
    Objects.requireNonNull(dto, "TrustmarkIssuerDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("entityId", dto.getEntityId());

    this.v.required()
        .build()
        .ifFailThrow("active", dto.getActive());

    this.v.required()
        .duration()
        .build()
        .ifFailThrow("trustMarkTokenValidityDuration", dto.getTrustMarkTokenValidityDuration());
  }

  /**
   * Validates SubordinateDto.
   *
   * @param dto the subordinate DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final SubordinateDto dto) {
    Objects.requireNonNull(dto, "SubordinateDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("taImId", dto.getTaImId());

    this.v.required()
        .entityid()
        .build()
        .ifFailThrow("entityIdentifier", dto.getEntityIdentifier());

    this.v.jwks()
        .build()
        .ifFailThrow("jwks", dto.getJwks());

    this.v.url()
        .build()
        .ifFailThrow("ecLocation", dto.getEcLocation());

    this.v.length(MIN_POLICY_CRIT_LENGTH, MAX_POLICY_CRIT_LENGTH)
        .build()
        .ifFailThrow("metadataPolicyCrit", dto.getMetadataPolicyCrit());

    this.v.length(MIN_POLICY_CRIT_LENGTH, MAX_POLICY_CRIT_LENGTH)
        .build()
        .ifFailThrow("crit", dto.getCrit());

    this.v.oidfPolicy()
        .build()
        .ifFailThrow("metadataPolicy", dto.getMetadataPolicy());
  }

  private void validatePolicyJson(final Object policy) {
    try {
      final String policyJson = MAPPER.writeValueAsString(policy);
      this.v.required()
          .json()
          .build()
          .ifFailThrow("policy", policyJson);
    }
    catch (final JacksonException e) {
      throw new PropertyValidationFailException("policy", policy.toString(),
          "Invalid JSON format: " + e.getMessage());
    }
  }
}

