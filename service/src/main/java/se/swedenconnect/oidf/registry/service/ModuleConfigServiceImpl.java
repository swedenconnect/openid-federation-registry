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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.api.dto.EntityToDto;
import se.swedenconnect.oidf.registry.api.dto.ResolverDto;
import se.swedenconnect.oidf.registry.api.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.api.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.api.dto.input.ResolverInputDto;
import se.swedenconnect.oidf.registry.api.dto.input.TrustAnchorInputDto;
import se.swedenconnect.oidf.registry.api.dto.input.TrustmarkInputDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.entity.EntityKeyType;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.ModuleEntity;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.EntityRepository;
import se.swedenconnect.oidf.registry.repository.ModuleRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;

import java.util.UUID;

/**
 * Default implementation of {@link ModuleConfigService} for TrustAnchor, Resolver and Trustmark modules.
 *
 * @author Per Fredrik Plars
 */
@Service
public class ModuleConfigServiceImpl implements ModuleConfigService {

  private final ModuleRepository moduleRepository;
  private final EntityRepository entityRepository;
  private final TrustMarkRepository trustMarkRepository;
  private final OrganizationService organizationService;

  public ModuleConfigServiceImpl(final ModuleRepository moduleRepository,
      final EntityRepository entityRepository,
      final TrustMarkRepository trustMarkRepository,
      final OrganizationService organizationService) {
    this.moduleRepository = moduleRepository;
    this.entityRepository = entityRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.organizationService = organizationService;
  }

  private OrganizationEntity resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(
        organizationRecord.orgNumber(), organizationRecord.orgName());
  }

  private EntityEntity findFederationEntityOrThrow(final OrganizationRecord organizationRecord,
      final UUID entityId) {
    return this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), entityId, EntityKeyType.FEDERATION_ENTITY)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.RELATION_NOT_FOUND,
            "No federation entity found for id %s".formatted(entityId)));
  }

  private ModuleEntity findModuleOrThrow(final OrganizationRecord organizationRecord,
      final UUID id, final FkKeyType type) {
    return this.moduleRepository.findByOrgNumberAndModuleIdAndModuleType(
            organizationRecord.orgNumber(), id, type.name())
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND,
            "No module of type %s found for id %s".formatted(type, id)));
  }

  // ---------------------------------------------------------------------------
  // TrustAnchor
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public TrustAnchorDto createTrustAnchor(final OrganizationRecord organizationRecord,
      final UUID id, final TrustAnchorInputDto input) {

    // entityId is a database UUID to EntityEntity
    final UUID entityId = UUID.fromString(input.getEntityId());
    final EntityEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final OrganizationEntity org = this.resolveOrganization(organizationRecord);

    if (entityEntity.getModuleByType(FkKeyType.TRUSTANCHOR).isPresent()) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "A TRUSTANCHOR module already exists for entity %s".formatted(entityEntity.getEntityId()));
    }

    final ModuleEntity module = EntityToDto.toEntity(id, input, entityEntity, org);

    this.moduleRepository.save(module);
    return EntityToDto.toDto(module);
  }

  @Override
  @Transactional
  public TrustAnchorDto updateTrustAnchor(final OrganizationRecord organizationRecord,
      final UUID id, final TrustAnchorInputDto input) {

    final ModuleEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.TRUSTANCHOR);

    EntityToDto.updateModuleEntity(module, input);
    this.moduleRepository.save(module);
    return EntityToDto.toDto(module);
  }

  @Override
  @Transactional(readOnly = true)
  public TrustAnchorDto getTrustAnchor(final OrganizationRecord organizationRecord, final UUID id) {
    final ModuleEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.TRUSTANCHOR);
    return EntityToDto.toDto(module);
  }

  @Override
  @Transactional
  public void deleteTrustAnchor(final OrganizationRecord organizationRecord, final UUID id) {
    final ModuleEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.TRUSTANCHOR);
    this.moduleRepository.delete(module);
  }

  // ---------------------------------------------------------------------------
  // Resolver
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public ResolverDto createResolver(final OrganizationRecord organizationRecord,
      final UUID id, final ResolverInputDto input) {

    final UUID entityId = UUID.fromString(input.getEntityId());
    final EntityEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final OrganizationEntity org = this.resolveOrganization(organizationRecord);

    if (entityEntity.getModuleByType(FkKeyType.RESOLVER).isPresent()) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "A RESOLVER module already exists for entity %s".formatted(entityEntity.getEntityId()));
    }

    final ModuleEntity module = EntityToDto.toEntity(id, input, entityEntity, org);

    this.moduleRepository.save(module);
    return EntityToDto.toDtoResolver(module);
  }

  @Override
  @Transactional
  public ResolverDto updateResolver(final OrganizationRecord organizationRecord,
      final UUID id, final ResolverInputDto input) {

    final ModuleEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.RESOLVER);

    EntityToDto.updateModuleEntity(module, input);
    this.moduleRepository.save(module);
    return EntityToDto.toDtoResolver(module);
  }

  @Override
  @Transactional(readOnly = true)
  public ResolverDto getResolver(final OrganizationRecord organizationRecord, final UUID id) {
    final ModuleEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.RESOLVER);
    return EntityToDto.toDtoResolver(module);
  }

  @Override
  @Transactional
  public void deleteResolver(final OrganizationRecord organizationRecord, final UUID id) {
    final ModuleEntity module = this.findModuleOrThrow(organizationRecord, id, FkKeyType.RESOLVER);
    this.moduleRepository.delete(module);
  }

  // ---------------------------------------------------------------------------
  // Trustmark
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public TrustmarkDto createTrustmark(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkInputDto input) {

    // trustmarkissuerId is a module UUID (TRUSTMARKISSUER)
    final UUID moduleId = UUID.fromString(input.getTrustmarkissuerId());
    final ModuleEntity issuerModule = this.moduleRepository
        .findByOrgNumberAndModuleIdAndModuleType(
            organizationRecord.orgNumber(), moduleId, FkKeyType.TRUSTMARKISSUER.name())
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.RELATION_NOT_FOUND,
            "No TRUSTMARKISSUER module found for id %s".formatted(moduleId)));

    final TrustMarkEntity entity = EntityToDto.toEntity(id, input, issuerModule);
    this.trustMarkRepository.save(entity);
    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public TrustmarkDto updateTrustmark(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkInputDto input) {

    final TrustMarkEntity existing = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));

    EntityToDto.updateEntity(existing, input);

    this.trustMarkRepository.save(existing);
    return EntityToDto.toDto(existing);
  }

  @Override
  @Transactional(readOnly = true)
  public TrustmarkDto getTrustmark(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkEntity entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));

    return EntityToDto.toDto(entity);
  }

  @Override
  @Transactional
  public void deleteTrustmark(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkEntity entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));
    this.trustMarkRepository.delete(entity);
  }
}


