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

package se.swedenconnect.oidf.registry.service;

import com.nimbusds.jose.jwk.JWKSet;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.registry.dto.EntityToDto;
import se.swedenconnect.oidf.registry.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.dto.OidfServiceHostedEntities;
import se.swedenconnect.oidf.registry.dto.PolicyDto;
import se.swedenconnect.oidf.registry.dto.SubordinateEntityDto;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The EntityResponseFormatter class provides functionality for formatting responses related to entities. It includes
 * support for constructing metadata and generating entity-specific details through processes like extracting
 * configuration settings and creating metadata entries for hosted entities.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class FederationMetadataCreator {

  /**
   * Creates a response map based on the provided {@code EntityEntity}. The response includes entity configuration
   * settings mapped by their keys and additional metadata for hosted entities. If the entity type is
   * {@code FEDERATION_ENTITY}, metadata is enriched with federation entity information. Ex: { "iss":
   * "http://oidf-registry.swedenconnect.se", "exp": 1742800981, "iat": 1742196181, "entity_records": [ {
   * "policy_record": {}, "hosted_record": { "metadata": { "federation_entity": { "organization_name": "anything",
   * "federation_fetch_endpoint": "https://dev.swedenconnect.se/oidf/im/fetch", "federation_list_endpoint":
   * "https://dev.swedenconnect.se/oidf/im/subordinate_listing" } } }, "subject": "http://dev.swedenconnect.se/oidf/im",
   * "issuer": "http://dev.swedenconnect.se/oidf/ta" } ], "jti": "d4f50aa3-5565-495f-9adf-0f3c330f6e19" }
   *
   * @param entityEntity an {@code EntityEntity} object containing entity settings and metadata
   * @return a {@code Map<String, Object>} representing the response for the entity. If the entity type is
   *     {@code FEDERATION_ENTITY}, the return value contains metadata specific to hosted entities. Otherwise, it
   *     contains the mapped entity settings.
   */
  public List<OidfServiceHostedEntities.Record> createEntityResponseV2(final EntityEntity entityEntity) {

    final OidfServiceHostedEntities.Record.RecordBuilder entityData = OidfServiceHostedEntities.Record.builder();

    entityData.policyRecord(OidfServiceHostedEntities.Record.PolicyRecord.builder().build());

    Optional.ofNullable(entityEntity.getPolicyEntity())
        .ifPresent(policyEntity -> {
          final PolicyDto policyDto = EntityToDto.toDtoPolicy(policyEntity);
          entityData.policyRecord(
              OidfServiceHostedEntities.Record.PolicyRecord
                  .builder()
                  .policyRecordId(policyDto.getPolicyId().toString())
                  .policy(policyDto.getPolicy()).build());
        });

    final EntityKeyType entityType = entityEntity.getEntityType();

    if (entityType == EntityKeyType.HOSTED_ENTITY) {

      final HostedEntityDto dto = EntityToDto.toDtoHosted(entityEntity);

      final String id = entityEntity.getIssuer()
          .replace("https://", "")
          .replace("http://", "")
          .replace(".", "_")
          .replace("/", "_");

      String calculatedEcLocation = entityEntity.getSubject().endsWith("/") ?
          entityEntity.getSubject() : entityEntity.getSubject() + "/";

      calculatedEcLocation += id + "/.well-known/openid-federation";

      entityData.subject(dto.getIssuer())
          .issuer(dto.getIssuer())
          .overrideConfigurationLocation(calculatedEcLocation)
          .crit(List.of("ec_location", "configuration_location_override"));

      final OidfServiceHostedEntities.Record record = entityData.build();

      final OidfServiceHostedEntities.Record hosted = OidfServiceHostedEntities.Record
          .builder()
          .issuer(record.getOverrideConfigurationLocation())
          .subject(record.getOverrideConfigurationLocation())
          .overrideConfigurationLocation(record.getOverrideConfigurationLocation())
          .crit(record.getCrit())
          .policyRecord(OidfServiceHostedEntities.Record.PolicyRecord.builder().build())
          .hostedRecord(OidfServiceHostedEntities.HostedRecord.builder().metadata(dto.getMetadata()).build())
          .build();

      return List.of(record, hosted);
    }

    if (entityType == EntityKeyType.SUBORDINATE_ENTITY) {
      final SubordinateEntityDto dto = EntityToDto.toDtoSubordinate(entityEntity);
      entityData.subject(dto.getSubject())
          .issuer(dto.getIssuer())
          .policyRecord(OidfServiceHostedEntities.Record.PolicyRecord.builder()
              .policyRecordId(dto.getPolicyId() == null ? null : dto.getPolicyId().toString())
              .policy(dto.getPolicy() == null ? Map.of() : dto.getPolicy())
              .build());


      if (dto.getJwks() != null && !dto.getJwks().isBlank()) {
        try {
          final JWKSet jwkSet = JWKSet.parse(dto.getJwks());
          entityData.jwks(jwkSet.toJSONObject());
        }
        catch (final ParseException e) {
          throw new IllegalArgumentException("Failed to parse JWKS", e);
        }
      }
      return List.of(entityData.build());
    }

    if (entityType == EntityKeyType.FEDERATION_ENTITY) {
      final FederationEntityDto dto = EntityToDto.toDtoPolicy(entityEntity);
      entityData.subject(dto.getIssuer())
          .issuer(dto.getIssuer())
          .crit(dto.getCrit())
          .metadataPolicyCrit(dto.getMetadataPolicyCrit());

      final Map<String, Object> federationEntityData = new HashMap<>();
      if (dto.getMetadata() != null) {
        federationEntityData.putAll(dto.getMetadata());
      }
      federationEntityData.put("federation_entity", this.createFederationMetadata(entityEntity));

      entityData.hostedRecord(OidfServiceHostedEntities
          .HostedRecord
          .builder()
          .metadata(federationEntityData)
          .build());
      return List.of(entityData.build());
    }

    return List.of(entityData.build());
  }

  protected OidfServiceHostedEntities.Metadata.FederationEntity createFederationMetadata(
      final EntityEntity entityEntity) {
    final String orgName = Optional.ofNullable(entityEntity.getOrganization().getOrgName())
        .orElseGet(() -> String.valueOf(entityEntity.getOrganization().getOrganizationId()));

    final OidfServiceHostedEntities.Metadata.FederationEntity.FederationEntityBuilder federationEntity =
        OidfServiceHostedEntities.Metadata.FederationEntity.builder();
    federationEntity.organizationName(orgName);

    final String sub = entityEntity.getSubject();

    Optional.ofNullable(entityEntity.getTrustanchorIntermediate())
        .ifPresent(moduleEntity -> {
          federationEntity.federationFetchEndpoint(String.format("%s/fetch", sub));
          federationEntity.federationListEndpoint(String.format("%s/subordinate_listing", sub));
        });

    Optional.ofNullable(entityEntity.getTrustanchorIntermediate())
        .ifPresent(moduleEntity -> {
          federationEntity.federationTrustMarkListEndpoint(String.format("%s/trust_mark_listing", sub));
          federationEntity.federationTrustMarkEndpoint(String.format("%s/trust_mark", sub));
          federationEntity.federationTrustMarkStatusEndpoint(String.format("%s/trust_mark_status", sub));

        });

    Optional.ofNullable(entityEntity.getTrustmarkIssuer())
        .ifPresent(moduleEntity -> {
          federationEntity.federationTrustMarkListEndpoint(String.format("%s/trust_mark_listing", sub));
          federationEntity.federationTrustMarkEndpoint(String.format("%s/trust_mark", sub));
          federationEntity.federationTrustMarkStatusEndpoint(String.format("%s/trust_mark_status", sub));

        });

    Optional.ofNullable(entityEntity.getResolver())
        .ifPresent(moduleEntity -> {
          federationEntity.federationResolveEndpoint(String.format("%s/resolve", sub));
          federationEntity.federationDiscoveryEndpoint(String.format("%s/discovery", sub));
        });

    return federationEntity.build();

  }

}
