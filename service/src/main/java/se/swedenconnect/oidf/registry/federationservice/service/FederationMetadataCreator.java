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
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.subordinate.model.Subordinate;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMarkSubject;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkSubjectRepository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
  private final String hostedPathSegment;
  private final Set<UUID> crossOrganizationInstances;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final Set<UUID> crossOrganizationTrustMarkInstances;

  /**
   * Create federation metadata
   *
   * @param subordinateRepository subordinateRepository
   * @param hostedPathSegment path segment of a calculated ec_location of a hosted entity, empty for none
   * @param crossOrganizationInstances the instances whose authority hints are resolved across every organization of
   *     the instance, empty for none
   * @param trustMarkSubjectRepository the repository of trust mark subjects, giving the trust marks an entity has
   *     been granted
   * @param crossOrganizationTrustMarkInstances the instances whose trust marks are resolved across every organization
   *     of the instance, empty for none
   */
  public FederationMetadataCreator(final SubordinateRepository subordinateRepository,
      final String hostedPathSegment,
      final Set<UUID> crossOrganizationInstances,
      final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final Set<UUID> crossOrganizationTrustMarkInstances) {
    this.subordinateRepository = subordinateRepository;
    this.hostedPathSegment = hostedPathSegment;
    this.crossOrganizationInstances = Set.copyOf(crossOrganizationInstances);
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.crossOrganizationTrustMarkInstances = Set.copyOf(crossOrganizationTrustMarkInstances);
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

      final HostedEntityDto dto = EntityToDtoMapper.toDtoHosted(entityEntity, this.hostedPathSegment);
        entityData.setEntityIdentifier(new EntityID(dto.getEntityIdentifier()));
        entityData.setMetadata(dto.getMetadata());
        entityData.setEcLocation(Optional.ofNullable(dto.getEffectiveEcLocation()).orElse(dto.getEntityIdentifier()));
        entityData.setCrit(EmptyToNull.list(dto.getCrit()));
      entityData.setTrustMarkSource(EmptyToNull.list(this.trustMarkSources(entityEntity, dto)));
      entityData.setAuthorityHints(EmptyToNull.list(this.authorityHint(entityEntity)));
      Optional.ofNullable(dto.getSigningKeyId())
          .filter(ids -> !ids.isEmpty())
          .map(ids -> String.join(",", ids))
          .ifPresent(entityData::setJwks);
        return entityData;
      }
    if (entityType == EntityType.FEDERATION_ENTITY) {
        if (!entityEntity.hasModules()) {
          return null;
        }
      final FederationEntityDto dto = EntityToDtoMapper.toFederationEntity(entityEntity, false);
        entityData.setEntityIdentifier(new EntityID(dto.getEntityIdentifier()));
        entityData.setEcLocation(dto.getEntityIdentifier());
        entityData.setCrit(EmptyToNull.list(dto.getCrit()));
        entityData.setMetadata(Map.of("federation_entity", this.createFederationMetadata(entityEntity)));
      Optional.ofNullable(dto.getSigningKeyId())
          .filter(ids -> !ids.isEmpty())
          .map(ids -> String.join(",", ids))
          .ifPresent(entityData::setJwks);
      entityData.setAuthorityHints(EmptyToNull.list(this.authorityHint(entityEntity)));

      //TODO Implement TrustMarkSources or TrustMarkIsssuers
      //entityData.setTrustMarkSource(
      // List.of(new TrustMarkSourceProperty(new EntityID("https://tmi"),"https://loa4")));
      return entityData;
      }

    throw new IllegalArgumentException("Unsupported entity type: " + entityType);

  }

  /**
   * The authority hints of an entity. The superiors with a subordinate statement pointing to the entity come first,
   * followed by the authority hints stored on the entity. Duplicates and empty hints are removed.
   * <p>
   * A subordinate belongs to the organization of its Trust Anchor or Intermediate, which is another organization than
   * the entity's own when the entity was registered under an Intermediate of another organization. An instance that
   * has turned on {@code cross_organization_authority_hints} therefore takes the superiors from the whole instance,
   * the same scope as the records exported for it. Without it the superiors come from the entity's own organization.
   *
   * @param entity Entity
   * @return the authority hints, empty if there are none
   */
  private List<String> authorityHint(final FederationEntity entity) {
    final Set<String> hints = new LinkedHashSet<>();
    final Organization organization = entity.getOrganization();
    final UUID instanceId = organization.getInstance().getInstanceId();
    final List<Subordinate> superiors = this.crossOrganizationInstances.contains(instanceId)
        ? this.subordinateRepository.findByInstanceIdAndEntityidentifier(instanceId, entity.getIssuer())
        : this.subordinateRepository.findByOrganizationIdAndEntityidentifier(
            organization.getOrganizationId(), entity.getIssuer());
    superiors
        .stream()
        .map(Subordinate::getTaIm)
        .map(taImEntity -> taImEntity.getEntity().getSubject())
        .forEach(hints::add);
    // Hints stored before they were validated may hold null or blank values, which must not break the export.
    Optional.ofNullable(entity.getAuthorityhints()).stream()
        .flatMap(List::stream)
        .filter(hint -> hint != null && !hint.isBlank())
        .forEach(hints::add);
    return List.copyOf(hints);
  }

  /**
   * The trust mark sources of a hosted entity: the sources stored on the entity, which an operator maintains and
   * which may name an issuer outside the registry, followed by the trust marks the registry has granted the entity.
   * A granted trust mark is a subject entry on a trust mark of the registry that is not revoked; for how long it is
   * valid is left to the issuer. Duplicates are dropped, so a granted trust mark that is also stored is given once.
   *
   * @param entity the hosted entity
   * @param dto the hosted entity with its stored sources
   * @return the sources to publish, in order
   */
  private List<TrustMarkSourceProperty> trustMarkSources(final FederationEntity entity, final HostedEntityDto dto) {
    final Map<List<String>, TrustMarkSourceProperty> sources = new LinkedHashMap<>();
    dto.getTrustMarkSources().forEach(source -> add(sources, source.getTrustMarkIssuer(), source.getTrustmarkId()));
    this.grantedTrustMarks(entity).forEach(subject -> add(sources,
        subject.getTrustMark().getTrustmarkIssuer().getEntity().getSubject(),
        subject.getTrustMark().getTrustmarkType()));
    return List.copyOf(sources.values());
  }

  /** Adds a source, unless the issuer or the trust mark type is missing, or the pair is already there. */
  private static void add(final Map<List<String>, TrustMarkSourceProperty> sources, final String issuer,
      final String trustMarkType) {
    if (issuer == null || issuer.isBlank() || trustMarkType == null || trustMarkType.isBlank()) {
      return;
    }
    sources.putIfAbsent(List.of(issuer, trustMarkType),
        new TrustMarkSourceProperty(new EntityID(issuer), trustMarkType));
  }

  /**
   * The trust marks the registry has granted an entity, as the subject entries that are not revoked. The entries of
   * a trust mark issuer of another organization of the instance are included only when the instance is configured
   * for it, exactly as the authority hints are.
   */
  private List<TrustMarkSubject> grantedTrustMarks(final FederationEntity entity) {
    final Organization organization = entity.getOrganization();
    final UUID instanceId = organization.getInstance().getInstanceId();
    final List<TrustMarkSubject> granted = this.crossOrganizationTrustMarkInstances.contains(instanceId)
        ? this.trustMarkSubjectRepository.findByInstanceIdAndSubject(instanceId, entity.getIssuer())
        : this.trustMarkSubjectRepository.findByOrganizationIdAndSubject(organization.getOrganizationId(),
            entity.getIssuer());
    return granted.stream()
        .filter(subject -> !Boolean.TRUE.equals(subject.getRevoked()))
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
