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

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.registry.api.dto.OidfServiceHostedEntitys;
import se.swedenconnect.oidf.registry.domain.Entity;
import se.swedenconnect.oidf.registry.domain.EntityToDomain;
import se.swedenconnect.oidf.registry.domain.FederationEntity;
import se.swedenconnect.oidf.registry.domain.HostedEntity;
import se.swedenconnect.oidf.registry.domain.SubordinateEntity;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.FkKeyType;

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
  public OidfServiceHostedEntitys.Record.RecordBuilder createEntityResponseV2(final EntityEntity entityEntity) {

    final OidfServiceHostedEntitys.Record.RecordBuilder entityData = OidfServiceHostedEntitys.Record.builder();

    entityData.policyRecord(EntityToDomain.map(entityEntity.getPolicyEntity()).getPolicy());

    final Entity entity = EntityToDomain.map(entityEntity);

    entityData.subject(entity.getSubject().toString())
        .issuer(entity.getIssuer().toString());

    if (entity instanceof HostedEntity hostedEntity) {
      entityData.overrideConfigurationLocation("https://www.swedenconnect.se/test_ec_location");
      entityData.crit(List.of("ec_location"));
      //entityData.metadataPolicyCrit()
      return entityData;
    }

    if (entity instanceof SubordinateEntity subordinateEntity) {
      entityData.jwks(subordinateEntity.getJwks().toJSONObject());
      return entityData;
    }

    if (entity instanceof FederationEntity federationEntity) {
      final Map<String, Object> federationEntityData = new HashMap<>(federationEntity.getMetadata());
      federationEntityData.put("federation_entity", this.createFederationMetadata(entityEntity));

      entityData.hostedRecord(OidfServiceHostedEntitys
          .HostedRecord
          .builder()
          .metadata(federationEntityData)
          .build());
      return entityData;
    }

    return entityData;
  }

  protected OidfServiceHostedEntitys.Metadata.FederationEntity createFederationMetadata(
      final EntityEntity entityEntity) {
    final String orgName = Optional.ofNullable(entityEntity.getOrganization().getOrgName())
        .orElseGet(() -> String.valueOf(entityEntity.getOrganization().getOrganizationId()));

    final OidfServiceHostedEntitys.Metadata.FederationEntity.FederationEntityBuilder federationEntity =
        OidfServiceHostedEntitys.Metadata.FederationEntity.builder();
    federationEntity.organizationName(orgName);

    final String sub = entityEntity.getSubject();

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.INTERMEDIATE, FkKeyType.TRUSTANCHOR))
        .findFirst()
        .ifPresent(moduleEntity -> {
          federationEntity.federationFetchEndpoint(String.format("%s/fetch", sub));
          federationEntity.federationListEndpoint(String.format("%s/subordinate_listing", sub));
        });

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.TRUSTMARKISSUER))
        .findFirst()
        .ifPresent(moduleEntity -> {
          federationEntity.federationTrustMarkListEndpoint(String.format("%s/trust_mark_listing", sub));
          federationEntity.federationTrustMarkEndpoint(String.format("%s/trust_mark", sub));
          federationEntity.federationTrustMarkStatusEndpoint(String.format("%s/trust_mark_status", sub));

        });

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.RESOLVER))
        .findFirst()
        .ifPresent(moduleEntity -> {
          federationEntity.federationResolveEndpoint(String.format("%s/resolve", sub));
          federationEntity.federationDiscoveryEndpoint(String.format("%s/discovery", sub));
        });
    return federationEntity.build();

  }

}
