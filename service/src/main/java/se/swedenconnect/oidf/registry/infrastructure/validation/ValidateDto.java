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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorExternalTrustMarkDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorIssuerDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.organization.dto.CreateOrganizationDto;
import se.swedenconnect.oidf.registry.organization.dto.DomainRequestDto;
import se.swedenconnect.oidf.registry.organization.dto.PreValidatedTrustMarksDto;
import se.swedenconnect.oidf.registry.registrationflow.dto.RegistrationFlowDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationJoinRequestDto;
import se.swedenconnect.oidf.registry.subordinate.dto.ConstraintsDto;
import se.swedenconnect.oidf.registry.subordinate.dto.NamingConstraintsDto;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkSubjectDto;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;
import se.swedenconnect.oidf.registry.validation.PropertyValidators;
import se.swedenconnect.oidf.registry.validation.VariableValueResolver;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validator for DTO objects using PropertyValidators framework.
 *
 * @author Per Fredrik Plars
 */
public class ValidateDto {
  private static final JsonMapper MAPPER = new JsonMapper();
  private static final int MIN_ENTITY_NAME_LENGTH = 1;
  private static final int MAX_ENTITY_NAME_LENGTH = 255;
  private static final int MIN_CRIT_LENGTH = 1;
  private static final int MAX_CRIT_LENGTH = 500;
  private static final int MIN_POLICY_CRIT_LENGTH = 2;
  private static final int MAX_POLICY_CRIT_LENGTH = 150;
  private static final String ENTITY_PREFIX = "@{entityprefix}";
  private static final int MIN_DOMAIN_LENGTH = 1;
  private static final int MAX_DOMAIN_LENGTH = 255;
  private static final int MIN_LEGAL_NAME_LENGTH = 1;
  private static final int MAX_LEGAL_NAME_LENGTH = 255;

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
   * Validates RegistrationJoinRequestDto.
   *
   * @param dto the registration join request DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final RegistrationJoinRequestDto dto) {
    Objects.requireNonNull(dto, "RegistrationJoinRequestDto cannot be null");

    this.v.required()
        .uuid()
        .build()
        .ifFailThrow("joinId", dto.getJoinId());

    this.v.required()
        .entityid()
        .build()
        .ifFailThrow("entityIdentifier", dto.getEntityIdentifier());

    Optional.ofNullable(dto.getTrustmarksRequested())
        .orElse(Collections.emptyList())
        .forEach(trustmarks -> {
          this.v.required()
              .entityid()
              .build()
              .ifFailThrow("trustmarkissuer", trustmarks.getTrustmarkIssuer());

          this.v.required()
              .entityid()
              .build()
              .ifFailThrow("trustmarktype", trustmarks.getTrustmarkType());
        });

  }

  /**
   * Validates DomainRequestDto. The domain is expected to already be trimmed and lower-cased by the caller, the
   * form in which it is stored.
   *
   * @param dto the domain request DTO
   * @param allowLocalhost whether {@code localhost} is an acceptable domain, mirroring
   *     {@code openid.federation.registry.entity-configuration-loader.enable-local-ip-address-ranges}
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final DomainRequestDto dto, final boolean allowLocalhost) {
    Objects.requireNonNull(dto, "DomainRequestDto cannot be null");

    this.v.required()
        .length(MIN_DOMAIN_LENGTH, MAX_DOMAIN_LENGTH)
        .domain(allowLocalhost)
        .build()
        .ifFailThrow("domain", dto.domain());
  }

  /**
   * Validates CreateOrganizationDto.
   *
   * @param dto the create organization DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final CreateOrganizationDto dto) {
    Objects.requireNonNull(dto, "CreateOrganizationDto cannot be null");

    this.v.required()
        .length(MIN_LEGAL_NAME_LENGTH, MAX_LEGAL_NAME_LENGTH)
        .build()
        .ifFailThrow("legalName", dto.legalName());
  }

  /**
   * Validates PreValidatedTrustMarksDto. A trust mark type is a URI naming the trust mark, so each entry is held
   * to the same shape as an entity identifier. An empty list is valid — it is how the operator withdraws every
   * pre-approval — but a blank entry is not, and neither is the same type listed twice: the stored list is a set,
   * so a duplicate is a mistake in the request rather than something to silently collapse.
   *
   * @param dto the pre-validated trust marks DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final PreValidatedTrustMarksDto dto) {
    Objects.requireNonNull(dto, "PreValidatedTrustMarksDto cannot be null");

    final List<String> trustMarkTypes = Optional.ofNullable(dto.preValidatedTrustMarks()).orElse(List.of());
    final Set<String> seen = new HashSet<>();
    trustMarkTypes.forEach(trustMarkType -> {
      this.v.required()
          .entityid()
          .build()
          .ifFailThrow("preValidatedTrustMarks", trustMarkType);
      if (!seen.add(trustMarkType)) {
        throw new PropertyValidationFailException("preValidatedTrustMarks", trustMarkType,
            "Trust mark type is listed more than once");
      }
    });
  }

  /**
   * Validates RegistrationFlowDto. The {@code flowId} field is intentionally not validated here because it is always
   * assigned server-side.
   *
   * @param dto the registration flow DTO
   * @throws PropertyValidationFailException if validation fails
   */
  public void validate(final RegistrationFlowDto dto) {
    Objects.requireNonNull(dto, "RegistrationFlowDto cannot be null");

    this.v.required()
        .build()
        .ifFailThrow("name", dto.name());

    this.v.required()
        .build()
        .ifFailThrow("description", dto.description());

    if (dto.flowType() !=
        se.swedenconnect.oidf.registry.registrationflow.process.step.Step.FlowType.TRUST_MARK_ISSUER) {
      this.v.required()
          .build()
          .ifFailThrow("technology", dto.technology());
    }

    this.v.required().build()
        .ifFailThrow("steps", dto.steps());

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

    this.v.length(MIN_ENTITY_NAME_LENGTH, MAX_ENTITY_NAME_LENGTH)
        .build()
        .ifFailThrow("name", dto.getName());

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

    this.validateTrustMarkIssuers(dto.getTrustMarkIssuers());
    this.validateExternalTrustMarks(dto);
  }

  /**
   * Validates the trust mark issuers of a trust anchor. An issuer is an entity identifier and is listed once. With
   * auto set the trust marks of the issuer are included automatically and no trust mark types are given. Without
   * auto at least one trust mark type is required.
   */
  private void validateTrustMarkIssuers(final List<TrustAnchorIssuerDto> issuers) {
    if (issuers == null || issuers.isEmpty()) {
      return;
    }
    final Set<String> seen = new HashSet<>();
    for (int i = 0; i < issuers.size(); i++) {
      final TrustAnchorIssuerDto issuer = issuers.get(i);
      final String key = "trustMarkIssuers[" + i + "]";
      if (issuer == null) {
        throw new PropertyValidationFailException(key, "An issuer is required");
      }

      this.v.required()
          .entityid()
          .build()
          .ifFailThrow(key + ".issuer", issuer.getIssuer());
      if (!seen.add(issuer.getIssuer().trim())) {
        throw new PropertyValidationFailException(key + ".issuer", issuer.getIssuer(),
            "The issuer is listed more than once");
      }

      final List<String> types = issuer.getTrustMarkTypes() == null ? List.of() : issuer.getTrustMarkTypes();
      if (issuer.isAuto()) {
        if (!types.isEmpty()) {
          throw new PropertyValidationFailException(key + ".trustMarkTypes", types.toString(),
              "No trust mark types are given when auto is set, every trust mark of the issuer is included");
        }
        continue;
      }
      if (types.isEmpty()) {
        throw new PropertyValidationFailException(key + ".trustMarkTypes",
            "At least one trust mark type is required unless auto is set");
      }
      this.v.required()
          .url()
          .build()
          .ifFailThrow(key + ".trustMarkTypes", types);
    }
  }

  /**
   * Validates the trust mark types of a trust anchor that have issuers outside the organization. A trust mark type is
   * listed once and is a URL. With allowAll no issuers are given, because anyone may issue the type. Without allowAll
   * at least one issuer is required, and the issuers are entity identifiers listed once. A trust mark type that anyone
   * may issue can not also be listed with the trust mark types of an issuer of the organization, since that would
   * limit who may issue it.
   */
  private void validateExternalTrustMarks(final TrustAnchorDto dto) {
    final List<TrustAnchorExternalTrustMarkDto> trustMarks = dto.getExternalTrustMarks();
    if (trustMarks == null || trustMarks.isEmpty()) {
      return;
    }
    final Set<String> seen = new HashSet<>();
    final Set<String> allowAllTypes = new HashSet<>();
    for (int i = 0; i < trustMarks.size(); i++) {
      final TrustAnchorExternalTrustMarkDto trustMark = trustMarks.get(i);
      final String key = "externalTrustMarks[" + i + "]";
      if (trustMark == null) {
        throw new PropertyValidationFailException(key, "A trust mark type is required");
      }

      this.v.required()
          .url()
          .build()
          .ifFailThrow(key + ".trustMarkType", trustMark.getTrustMarkType());
      final String type = trustMark.getTrustMarkType().trim();
      if (!seen.add(type)) {
        throw new PropertyValidationFailException(key + ".trustMarkType", type,
            "The trust mark type is listed more than once");
      }

      final List<String> issuers = trustMark.getIssuers() == null ? List.of() : trustMark.getIssuers();
      if (trustMark.isAllowAll()) {
        if (!issuers.isEmpty()) {
          throw new PropertyValidationFailException(key + ".issuers", issuers.toString(),
              "No issuers are given when allowAll is set, anyone may issue the trust mark type");
        }
        allowAllTypes.add(type);
        continue;
      }
      if (issuers.isEmpty()) {
        throw new PropertyValidationFailException(key + ".issuers",
            "At least one issuer is required unless allowAll is set");
      }
      this.v.required()
          .entityid()
          .build()
          .ifFailThrow(key + ".issuers", issuers);
      if (issuers.stream().map(String::trim).distinct().count() != issuers.size()) {
        throw new PropertyValidationFailException(key + ".issuers", issuers.toString(),
            "An issuer is listed more than once");
      }
    }

    if (!allowAllTypes.isEmpty() && dto.getTrustMarkIssuers() != null) {
      for (final TrustAnchorIssuerDto issuer : dto.getTrustMarkIssuers()) {
        if (issuer == null || issuer.isAuto() || issuer.getTrustMarkTypes() == null) {
          continue;
        }
        for (final String type : issuer.getTrustMarkTypes()) {
          if (allowAllTypes.contains(type.trim())) {
            throw new PropertyValidationFailException("externalTrustMarks", type,
                "The trust mark type is allowed for all issuers and can not also be listed for the issuer "
                    + issuer.getIssuer());
          }
        }
      }
    }
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

    // Optional, no duration means that the trust marks never expire.
    this.v.duration()
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

    validateConstraints(dto.getConstraints());
  }

  /**
   * Validates constraints for a subordinate statement, see OpenID Federation 1.0 section 6.2. Nothing is required.
   * What is given must be a non-negative max_path_length and lists of strings that are not empty.
   *
   * @param constraints the constraints, may be null
   * @throws PropertyValidationFailException if validation fails
   */
  static void validateConstraints(final ConstraintsDto constraints) {
    if (constraints == null) {
      return;
    }
    final Integer maxPathLength = constraints.getMaxPathLength();
    if (maxPathLength != null && maxPathLength < 0) {
      throw new PropertyValidationFailException("constraints.max_path_length", String.valueOf(maxPathLength),
          "Expected a non-negative integer");
    }
    final NamingConstraintsDto naming = constraints.getNamingConstraints();
    if (naming != null) {
      validateStringList("constraints.naming_constraints.permitted", naming.getPermitted());
      validateStringList("constraints.naming_constraints.excluded", naming.getExcluded());
    }
    validateStringList("constraints.allowed_entity_types", constraints.getAllowedEntityTypes());
  }

  private static void validateStringList(final String key, final List<String> list) {
    if (list != null && list.stream().anyMatch(item -> item == null || item.isBlank())) {
      throw new PropertyValidationFailException(key, "Expected an array of strings that are not empty");
    }
  }

}

