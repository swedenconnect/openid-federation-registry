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

package se.swedenconnect.oidf.entity.registry.service;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.entity.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static se.swedenconnect.oidf.entity.registry.service.FederationApiService.FEDERATION_ENTITY_ATT;
import static se.swedenconnect.oidf.entity.registry.service.FederationApiService.HOSTED_RECORD_ATT;
import static se.swedenconnect.oidf.entity.registry.service.FederationApiService.METADATA_ATT;

/**
 * The EntityResponseFormatter class provides functionality for formatting responses related to entities. It includes
 * support for constructing metadata and generating entity-specific details through processes like extracting
 * configuration settings and creating metadata entries for hosted entities.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class EntityResponseFormatter {
  /**
   * Enum representing various types of metadata used within the system, each associated with a specific metadata key
   * and a corresponding format string for constructing metadata URLs based on a base URL.
   */
  enum MetadataType {
    TRUST_MARK_STATUS("trust_mark_status", "%s/trust-mark-issuer/trust_mark_status"),
    TRUST_MARK_LISTING("trust_mark_listing", "%s/trust-mark-issuer/trust_mark_listing"),
    TRUST_MARK("trust_mark", "%s/trust-mark-issuer/trust_mark"),
    RESOLVE("resolve", "%s/trust-anchor/resolve"),
    DISCOVERY("discovery", "%s/trust-anchor/discovery"),
    FETCH("fetch", "%s/intermediate/fetch"),
    SUBORDINATE_LISTING("subordinate_listing", "%s/intermediate/subordinate_listing"),
    ORGANIZATION_NAME("organization_name", "%s");

    final String name;
    final String value;

    MetadataType(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    /**
     * Sets a metadata entry for this type by formatting the value with the given base URL.
     *
     * @param baseUrl the base URL to be used for formatting the metadata value
     * @param metadata the map where the metadata entry will be stored; the key is the name of this type, and the
     *     value is the formatted string
     */
    public void set(final String baseUrl, final Map<String, Object> metadata) {
      metadata.put(this.name, String.format(this.value, baseUrl));
    }

  }

  /**
   * Creates a response map based on the provided {@code EntityEntity}. The response includes entity configuration
   * settings mapped by their keys and additional metadata for hosted entities. If the entity type is
   * {@code HOSTED_ENTITY}, metadata is enriched with federation entity information. Ex: { "iss":
   * "http://oidf-registry.swedenconnect.se", "exp": 1742800981, "iat": 1742196181, "entity_records": [ {
   * "policy_record": {}, "hosted_record": { "metadata": { "federation_entity": { "organization_name": "anything",
   * "federation_fetch_endpoint": "https://dev.swedenconnect.se/oidf/im/fetch", "federation_list_endpoint":
   * "https://dev.swedenconnect.se/oidf/im/subordinate_listing" } } }, "subject": "http://dev.swedenconnect.se/oidf/im",
   * "issuer": "http://dev.swedenconnect.se/oidf/ta" } ], "jti": "d4f50aa3-5565-495f-9adf-0f3c330f6e19" }
   *
   * @param entityEntity an {@code EntityEntity} object containing entity settings and metadata
   * @return a {@code Map<String, Object>} representing the response for the entity. If the entity type is
   *     {@code HOSTED_ENTITY}, the return value contains metadata specific to hosted entities. Otherwise, it contains
   *     the mapped entity settings.
   */
  public Map<String, Object> createEntityResponse(final EntityEntity entityEntity) {

    final Map<String, Object> entityData = entityEntity.getSettingsEntityList()
        .stream()
        .collect(Collectors.toMap(
            SettingsEntity::getKey,
            SettingsEntity::castValue
        ));

    if (entityEntity.getEntityType() == EntityKeyType.HOSTED_ENTITY) {
      entityData.remove(METADATA_ATT);
      final Map<String, Object> metadata = new HashMap<>();

      entityEntity.getSettingsEntity(METADATA_ATT)
          .map(SettingsEntity::castValue)
          .ifPresent(o -> metadata.putAll((Map<String, Object>) o));

      if (metadata.containsKey(FEDERATION_ENTITY_ATT)) {
        log.info("Metadata attribute: {} will be overwritten for entity: {}",
            FEDERATION_ENTITY_ATT, entityEntity.getEntityId());
      }
      metadata.put(FEDERATION_ENTITY_ATT, this.createFederationEntity(entityEntity));

      entityData.put(HOSTED_RECORD_ATT,
          Map.of(METADATA_ATT, metadata));

      return entityData;
    }
    return entityData;
  }

  protected Map<String, Object> createFederationEntity(final EntityEntity entityEntity) {
    final String orgName = Optional.ofNullable(entityEntity.getOrganization().getOrgName())
        .orElseGet(() -> String.valueOf(entityEntity.getOrganization().getOrganizationId()));
    final Map<String, Object> federationEntity = new HashMap<>();
    federationEntity.put("organization_name", orgName);
    final String sub = entityEntity.getSubject();

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.INTERMEDIATE, FkKeyType.TRUSTANCHOR))
        .findFirst()
        .ifPresent(moduleEntity -> {
          MetadataType.FETCH.set(sub, federationEntity);
          MetadataType.SUBORDINATE_LISTING.set(sub, federationEntity);
        });

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.TRUSTMARKISSUER))
        .findFirst()
        .ifPresent(moduleEntity -> {
          MetadataType.TRUST_MARK.set(sub, federationEntity);
          MetadataType.TRUST_MARK_LISTING.set(sub, federationEntity);
          MetadataType.TRUST_MARK_STATUS.set(sub, federationEntity);
        });

    entityEntity.getModules()
        .stream()
        .filter(moduleEntity -> moduleEntity.isOfType(FkKeyType.RESOLVER))
        .findFirst()
        .ifPresent(moduleEntity -> {
          MetadataType.RESOLVE.set(sub, federationEntity);
          MetadataType.DISCOVERY.set(sub, federationEntity);
        });
    return federationEntity;

  }

}
