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

package se.swedenconnect.oidf.registry.federationservice.service;

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.registry.entity.dto.FederationEntityDto;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.mapper.EntityToDtoMapper;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.model.FederationMetadata;
import se.swedenconnect.oidf.registry.federationservice.model.TrustMarkSourceProperty;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;

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

  final SubordinateRepository subordinateRepository;

  /**
   * Create federation metadata
   *
   * @param subordinateRepository subordinateRepository
   */
  public FederationMetadataCreator(final SubordinateRepository subordinateRepository) {
    this.subordinateRepository = subordinateRepository;
  }

  /**
   * Creates a response map based on the provided {@code FederationEntity}. The response includes entity configuration
   * settings mapped by their keys and additional metadata for hosted entities. If the entity type is
   * {@code FEDERATION_ENTITY}, metadata is enriched with federation entity information. Ex: { "iss":
   * "http://oidf-registry.swedenconnect.se", "exp": 1742800981, "iat": 1742196181, "entity_records": [ {
   * "policy_record": {}, "hosted_record": { "metadata": { "federation_entity": { "organization_name": "anything",
   * "federation_fetch_endpoint": "https://dev.swedenconnect.se/oidf/im/fetch", "federation_list_endpoint":
   * "https://dev.swedenconnect.se/oidf/im/subordinate_listing" } } }, "subject": "http://dev.swedenconnect.se/oidf/im",
   * "issuer": "http://dev.swedenconnect.se/oidf/ta" } ], "jti": "d4f50aa3-5565-495f-9adf-0f3c330f6e19" }
   *
   * @param entityEntity an {@code FederationEntity} object containing entity settings and metadata
   * @return a {@code Map<String, Object>} representing the response for the entity. If the entity type is
   *     {@code FEDERATION_ENTITY}, the return value contains metadata specific to hosted entities. Otherwise, it
   *     contains the mapped entity settings.
   */
  public EntityRecord createEntityResponse(final FederationEntity entityEntity) {
    final EntityRecord entityData = new EntityRecord();
    final EntityType entityType = entityEntity.getEntityType();

    if (entityType == EntityType.HOSTED_ENTITY) {

      final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entityEntity);
        entityData.setEntityIdentifier(new EntityID(dto.getEntityIdentifier()));
        entityData.setMetadata(dto.getMetadata());
        entityData.setEcLocation(dto.getEffectiveEcLocation());
        entityData.setCrit(dto.getCrit());
      entityData.setTrustMarkSource(dto.getTrustMarkSources().stream()
          .map(trustmarkSourceDto ->
              new TrustMarkSourceProperty(new EntityID(trustmarkSourceDto.getTrustMarkIssuer()),
                  trustmarkSourceDto.getTrustmarkId())).toList());
      entityData.setAuthorityHints(this.authorityHint(entityEntity));
        return entityData;
      }
    if (entityType == EntityType.FEDERATION_ENTITY) {
        if (!entityEntity.hasModules()) {
          return null;
        }
      final FederationEntityDto dto = EntityToDtoMapper.toFederationEntity(entityEntity, false);
        entityData.setEntityIdentifier(new EntityID(dto.getEntityIdentifier()));
        entityData.setCrit(dto.getCrit());
        entityData.setMetadata(Map.of("federation_entity", this.createFederationMetadata(entityEntity)));
      entityData.setAuthorityHints(this.authorityHint(entityEntity));

      return entityData;
      }

    throw new IllegalArgumentException("Unsupported entity type: " + entityType);

  }

  /**
   * Trying to find a subordinate statement that are pointing to this entity. If true the superior entity will be set as
   * a authorityHint
   *
   * @param entity Entity
   * @return Issuer for subordinate statement if there is one.
   */
  private List<String> authorityHint(final FederationEntity entity) {
    return this.subordinateRepository.findByOrgNumberAndEntityidentifier(entity.getOrganization().getOrgNumber(),
            entity.getIssuer())
        .stream()
        .map(Subordinate::getTaIm)
        .map(taImEntity -> taImEntity.getEntity().getSubject())
        .toList();

  }

  protected FederationMetadata createFederationMetadata(final FederationEntity entityEntity) {

    final String orgName = Optional.ofNullable(entityEntity.getOrganization().getOrgName())
        .orElseGet(() -> String.valueOf(entityEntity.getOrganization().getOrganizationId()));

    final FederationMetadata.FederationMetadataBuilder federationEntity = FederationMetadata.builder();
    federationEntity.organizationName(orgName);

    final String sub = entityEntity.getIssuer();

    Optional.ofNullable(entityEntity.getTrustanchorIntermediate())
        .ifPresent(moduleEntity -> {
          federationEntity.federationFetchEndpoint(String.format("%s/fetch", sub));
          federationEntity.federationListEndpoint(String.format("%s/subordinate_listing", sub));
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
