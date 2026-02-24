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

package se.swedenconnect.oidf.registry.module.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.model.EntityType;
import se.swedenconnect.oidf.registry.entity.model.FederationEntity;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.infrastructure.validation.ValidateDto;
import se.swedenconnect.oidf.registry.module.dto.IntermediateDto;
import se.swedenconnect.oidf.registry.module.dto.ModuleDto;
import se.swedenconnect.oidf.registry.module.dto.ResolverDto;
import se.swedenconnect.oidf.registry.module.dto.TrustAnchorDto;
import se.swedenconnect.oidf.registry.module.dto.TrustmarkIssuerDto;
import se.swedenconnect.oidf.registry.module.mapper.DtoToModuleMapper;
import se.swedenconnect.oidf.registry.module.mapper.ModuleToDtoMapper;
import se.swedenconnect.oidf.registry.module.model.ModuleType;
import se.swedenconnect.oidf.registry.module.model.Resolver;
import se.swedenconnect.oidf.registry.module.model.TrustAnchorIntermediateModule;
import se.swedenconnect.oidf.registry.module.model.TrustMarkIssuer;
import se.swedenconnect.oidf.registry.module.repository.ResolverRepository;
import se.swedenconnect.oidf.registry.module.repository.TaImRepository;
import se.swedenconnect.oidf.registry.module.repository.TrustmarkIssuerRepository;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkDto;
import se.swedenconnect.oidf.registry.trustmark.dto.TrustmarkWithSubjectsDto;
import se.swedenconnect.oidf.registry.trustmark.mapper.DtoToTrustmarkMapper;
import se.swedenconnect.oidf.registry.trustmark.mapper.TrustmarkToDtoMapper;
import se.swedenconnect.oidf.registry.trustmark.model.TrustMark;
import se.swedenconnect.oidf.registry.trustmark.repository.TrustMarkRepository;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link ModuleConfigService} for TrustAnchor, Resolver and Trustmark modules.
 *
 * @author Per Fredrik Plars
 */
@Service
public class ModuleConfigServiceImpl implements ModuleConfigService {

  private final TaImRepository moduleRepository;
  private final EntityRepository entityRepository;
  private final TrustMarkRepository trustMarkRepository;
  private final TrustmarkIssuerRepository trustmarkIssuerRepository;
  private final ResolverRepository resolverRepository;
  private final OrganizationService organizationService;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param moduleRepository the module repository
   * @param entityRepository the entity repository
   * @param trustMarkRepository the trust mark repository
   * @param trustmarkIssuerRepository the trustmark issuer repository
   * @param resolverRepository the resolver repository
   * @param organizationService the organization service
   * @param auditService the audit service
   */
  public ModuleConfigServiceImpl(final TaImRepository moduleRepository,
      final EntityRepository entityRepository,
      final TrustMarkRepository trustMarkRepository,
      final TrustmarkIssuerRepository trustmarkIssuerRepository,
      final ResolverRepository resolverRepository,
      final OrganizationService organizationService,
      final RegistryAuditService auditService) {
    this.moduleRepository = moduleRepository;
    this.entityRepository = entityRepository;
    this.trustMarkRepository = trustMarkRepository;
    this.trustmarkIssuerRepository = trustmarkIssuerRepository;
    this.resolverRepository = resolverRepository;
    this.organizationService = organizationService;
    this.auditService = auditService;
  }

  private Organization resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(
        organizationRecord.orgNumber(), organizationRecord.orgName());
  }

  private FederationEntity findFederationEntityOrThrow(final OrganizationRecord organizationRecord,
      final UUID entityId) {
    return this.entityRepository.findByOrgNumberAndEntityIdAndEntityKeyType(
            organizationRecord.orgNumber(), entityId, EntityType.FEDERATION_ENTITY)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.RELATION_NOT_FOUND,
            "No federation entity found for id %s".formatted(entityId)));
  }

  private TrustAnchorIntermediateModule findModuleOrThrow(final OrganizationRecord organizationRecord,
      final UUID id, final ModuleType type) {
    return this.moduleRepository.findByOrgNumberAndTaImIdAndModuleType(
            organizationRecord.orgNumber(), id, type)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND,
            "No module of type %s found for id %s".formatted(type, id)));
  }

  /**
   * Creates a trust anchor.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   * @param input the trust anchor data
   * @return the created trust anchor
   */
  @Override
  @Transactional
  public TrustAnchorDto createTrustAnchor(final OrganizationRecord organizationRecord,
      final UUID id, final TrustAnchorDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    // entityId is a database UUID to FederationEntity
    final UUID entityId = input.getEntityId();
    final FederationEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final Organization org = this.resolveOrganization(organizationRecord);

    if (entityEntity.getTrustanchorIntermediate() != null) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "A INTERMEDIATE or TRUSTANCHOR module already exists for entity %s".formatted(entityEntity.getEntityId()));
    }

    final TrustAnchorIntermediateModule module = DtoToModuleMapper.toEntity(id, input, entityEntity, org);

    this.moduleRepository.save(module);
    final TrustAnchorDto dto = ModuleToDtoMapper.toDto(module);
    this.auditService.trustAnchorCreated(id, org.getInstance().getInstanceId(), org.getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates a trust anchor.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   * @param input the trust anchor data
   * @return the updated trust anchor
   */
  @Override
  @Transactional
  public TrustAnchorDto updateTrustAnchor(final OrganizationRecord organizationRecord,
      final UUID id, final TrustAnchorDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final TrustAnchorIntermediateModule module = this.findModuleOrThrow(organizationRecord, id, ModuleType.TRUSTANCHOR);
    final TrustAnchorDto oldDto = ModuleToDtoMapper.toDto(module);

    DtoToModuleMapper.updateIntermediate(module, input);
    this.moduleRepository.save(module);
    final TrustAnchorDto newDto = ModuleToDtoMapper.toDto(module);
    this.auditService.trustAnchorUpdated(id, module.getOrganization().getInstance().getInstanceId(),
        module.getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a trust anchor by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   * @return the trust anchor
   */
  @Override
  @Transactional(readOnly = true)
  public TrustAnchorDto getTrustAnchor(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustAnchorIntermediateModule module = this.findModuleOrThrow(organizationRecord, id, ModuleType.TRUSTANCHOR);
    return ModuleToDtoMapper.toDto(module);
  }

  /**
   * Deletes a trust anchor.
   *
   * @param organizationRecord the organization record
   * @param id the trust anchor ID
   */
  @Override
  @Transactional
  public void deleteTrustAnchor(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustAnchorIntermediateModule module = this.findModuleOrThrow(organizationRecord, id, ModuleType.TRUSTANCHOR);
    final TrustAnchorDto dto = ModuleToDtoMapper.toDto(module);
    module.getEntity().setTrustanchorIntermediate(null);
    this.moduleRepository.delete(module);
    this.auditService.trustAnchorDeleted(id, module.getOrganization().getInstance().getInstanceId(),
        module.getOrganization().getOrganizationId(), dto);
  }

  // ---------------------------------------------------------------------------
  // Intermediate
  // ---------------------------------------------------------------------------

  /**
   * Creates an intermediate.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   * @param input the intermediate data
   * @return the created intermediate
   */
  @Override
  @Transactional
  public IntermediateDto createIntermediate(final OrganizationRecord organizationRecord,
      final UUID id, final IntermediateDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    // entityId is a database UUID to FederationEntity
    final UUID entityId = input.getEntityId();
    final FederationEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final Organization org = this.resolveOrganization(organizationRecord);

    if (entityEntity.getTrustanchorIntermediate() != null) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "A INTERMEDIATE or TRUSTANCHOR module already exists for entity %s".formatted(entityEntity.getEntityId()));
    }

    final TrustAnchorIntermediateModule module = DtoToModuleMapper.toEntity(id, input, entityEntity, org);

    this.moduleRepository.save(module);
    final IntermediateDto dto = ModuleToDtoMapper.toDtoIntermediate(module);
    this.auditService.intermediateCreated(id, org.getInstance().getInstanceId(), org.getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates an intermediate.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   * @param input the intermediate data
   * @return the updated intermediate
   */
  @Override
  @Transactional
  public IntermediateDto updateIntermediate(final OrganizationRecord organizationRecord,
      final UUID id, final IntermediateDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final TrustAnchorIntermediateModule module =
        this.findModuleOrThrow(organizationRecord, id, ModuleType.INTERMEDIATE);
    final IntermediateDto oldDto = ModuleToDtoMapper.toDtoIntermediate(module);

    DtoToModuleMapper.updateIntermediate(module, input);
    this.moduleRepository.save(module);
    final IntermediateDto newDto = ModuleToDtoMapper.toDtoIntermediate(module);
    this.auditService.intermediateUpdated(id, module.getOrganization().getInstance().getInstanceId(),
        module.getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets an intermediate by ID.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   * @return the intermediate
   */
  @Override
  @Transactional(readOnly = true)
  public IntermediateDto getIntermediate(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustAnchorIntermediateModule module =
        this.findModuleOrThrow(organizationRecord, id, ModuleType.INTERMEDIATE);
    return ModuleToDtoMapper.toDtoIntermediate(module);
  }

  /**
   * Deletes an intermediate.
   *
   * @param organizationRecord the organization record
   * @param id the intermediate ID
   */
  @Override
  @Transactional
  public void deleteIntermediate(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustAnchorIntermediateModule module =
        this.findModuleOrThrow(organizationRecord, id, ModuleType.INTERMEDIATE);
    final IntermediateDto dto = ModuleToDtoMapper.toDtoIntermediate(module);
    this.moduleRepository.delete(module);
    this.auditService.intermediateDeleted(id, module.getOrganization().getInstance().getInstanceId(),
        module.getOrganization().getOrganizationId(), dto);
  }

  // ---------------------------------------------------------------------------
  // Resolver
  // ---------------------------------------------------------------------------

  private Resolver findResolverOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    return this.resolverRepository.findByOrgNumberAndResolverId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No resolver found for id %s".formatted(id)));
  }

  /**
   * Creates a resolver.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   * @param input the resolver data
   * @return the created resolver
   */
  @Override
  @Transactional
  public ResolverDto createResolver(final OrganizationRecord organizationRecord,
      final UUID id, final ResolverDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final UUID entityId = input.getEntityId();
    final FederationEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);
    final UUID orgId = entityEntity.getOrganization().getOrganizationId();

    final Resolver entity = DtoToModuleMapper.toEntity(id, input, entityEntity);

    this.resolverRepository.save(entity);
    final ResolverDto dto = ModuleToDtoMapper.toDto(entity);
    this.auditService.resolverCreated(id, entityEntity.getOrganization().getInstance().getInstanceId(), orgId,
        null, dto);
    return dto;
  }

  /**
   * Updates a resolver.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   * @param input the resolver data
   * @return the updated resolver
   */
  @Override
  @Transactional
  public ResolverDto updateResolver(final OrganizationRecord organizationRecord,
      final UUID id, final ResolverDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final Resolver existing = this.findResolverOrThrow(organizationRecord, id);
    final ResolverDto oldDto = ModuleToDtoMapper.toDto(existing);

    DtoToModuleMapper.updateEntity(existing, input);
    this.resolverRepository.save(existing);
    final ResolverDto newDto = ModuleToDtoMapper.toDto(existing);
    this.auditService.resolverUpdated(id, existing.getEntity().getOrganization().getInstance().getInstanceId(),
        existing.getEntity().getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a resolver by ID.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   * @return the resolver
   */
  @Override
  @Transactional(readOnly = true)
  public ResolverDto getResolver(final OrganizationRecord organizationRecord, final UUID id) {
    final Resolver entity = this.findResolverOrThrow(organizationRecord, id);
    return ModuleToDtoMapper.toDto(entity);
  }

  /**
   * Deletes a resolver.
   *
   * @param organizationRecord the organization record
   * @param id the resolver ID
   */
  @Override
  @Transactional
  public void deleteResolver(final OrganizationRecord organizationRecord, final UUID id) {
    final Resolver entity = this.findResolverOrThrow(organizationRecord, id);
    final UUID orgId = entity.getEntity().getOrganization().getOrganizationId();
    final ResolverDto dto = ModuleToDtoMapper.toDto(entity);
    entity.getEntity().setResolver(null);
    this.resolverRepository.delete(entity);
    this.auditService.resolverDeleted(id, entity.getEntity().getOrganization().getInstance().getInstanceId(),
        orgId, dto);
  }

  // ---------------------------------------------------------------------------
  // Trustmark
  // ---------------------------------------------------------------------------

  /**
   * Creates a trust mark.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   * @param input the trust mark data
   * @return the created trust mark
   */
  @Override
  @Transactional
  public TrustmarkDto createTrustmark(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    // trustmarkissuerId is a module UUID (TRUSTMARKISSUER)
    final UUID inputTrustmarkissuerId = input.getTrustmarkissuerId();
    final TrustMarkIssuer issuerModule = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), inputTrustmarkissuerId)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.RELATION_NOT_FOUND,
            "No TRUSTMARKISSUER  found for id %s".formatted(inputTrustmarkissuerId)));

    final TrustMark entity = DtoToTrustmarkMapper.toEntity(id, input, issuerModule);
    this.trustMarkRepository.save(entity);
    final TrustmarkDto dto = TrustmarkToDtoMapper.toDto(entity);
    this.auditService.trustmarkCreated(id, issuerModule.getEntity().getOrganization().getInstance().getInstanceId(),
        issuerModule.getEntity().getOrganization().getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates a trust mark.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   * @param input the trust mark data
   * @return the updated trust mark
   */
  @Override
  @Transactional
  public TrustmarkDto updateTrustmark(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final TrustMark existing = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));
    final TrustmarkDto oldDto = TrustmarkToDtoMapper.toDto(existing);

    DtoToTrustmarkMapper.updateEntity(existing, input);

    this.trustMarkRepository.save(existing);
    final TrustmarkDto newDto = TrustmarkToDtoMapper.toDto(existing);
    this.auditService.trustmarkUpdated(id,
        existing.getTrustmarkIssuer().getEntity().getOrganization().getInstance().getInstanceId(),
        existing.getTrustmarkIssuer().getEntity().getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a trust mark by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   * @return the trust mark
   */
  @Override
  @Transactional(readOnly = true)
  public TrustmarkDto getTrustmark(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMark entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));

    return TrustmarkToDtoMapper.toDto(entity);
  }

  /**
   * Deletes a trust mark.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark ID
   */
  @Override
  @Transactional
  public void deleteTrustmark(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMark entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(id)));
    final TrustmarkDto dto = TrustmarkToDtoMapper.toDto(entity);
    this.trustMarkRepository.delete(entity);
    this.auditService.trustmarkDeleted(id,
        entity.getTrustmarkIssuer().getEntity().getOrganization().getInstance().getInstanceId(),
        entity.getTrustmarkIssuer().getEntity().getOrganization().getOrganizationId(), dto);
  }

  // ---------------------------------------------------------------------------
  // Trustmark Issuer
  // ---------------------------------------------------------------------------

  /**
   * Creates a trust mark issuer.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   * @param input the trust mark issuer data
   * @return the created trust mark issuer
   */
  @Override
  @Transactional
  public TrustmarkIssuerDto createTrustmarkIssuer(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkIssuerDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final UUID entityId = input.getEntityId();
    final FederationEntity entityEntity = this.findFederationEntityOrThrow(organizationRecord, entityId);

    final TrustMarkIssuer entity = DtoToModuleMapper.toEntity(id, input, entityEntity);

    this.trustmarkIssuerRepository.save(entity);
    final TrustmarkIssuerDto dto = ModuleToDtoMapper.toDto(entity);
    this.auditService.trustmarkIssuerCreated(id, entityEntity.getOrganization().getInstance().getInstanceId(),
        entityEntity.getOrganization().getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates a trust mark issuer.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   * @param input the trust mark issuer data
   * @return the updated trust mark issuer
   */
  @Override
  @Transactional
  public TrustmarkIssuerDto updateTrustmarkIssuer(final OrganizationRecord organizationRecord,
      final UUID id, final TrustmarkIssuerDto input) {
    ValidateDto.init(organizationRecord).validate(input);

    final TrustMarkIssuer existing = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark issuer found for id %s".formatted(id)));
    final TrustmarkIssuerDto oldDto = ModuleToDtoMapper.toDto(existing);

    DtoToModuleMapper.updateEntity(existing, input);

    this.trustmarkIssuerRepository.save(existing);
    final TrustmarkIssuerDto newDto = ModuleToDtoMapper.toDto(existing);
    this.auditService.trustmarkIssuerUpdated(id, existing.getEntity().getOrganization().getInstance().getInstanceId(),
        existing.getEntity().getOrganization().getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Gets a trust mark issuer by ID.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   * @return the trust mark issuer
   */
  @Override
  @Transactional(readOnly = true)
  public TrustmarkIssuerDto getTrustmarkIssuer(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkIssuer entity = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark issuer found for id %s".formatted(id)));

    return ModuleToDtoMapper.toDto(entity);
  }

  /**
   * Deletes a trust mark issuer.
   *
   * @param organizationRecord the organization record
   * @param id the trust mark issuer ID
   */
  @Override
  @Transactional
  public void deleteTrustmarkIssuer(final OrganizationRecord organizationRecord, final UUID id) {
    final TrustMarkIssuer entity = this.trustmarkIssuerRepository
        .findByOrgNumberAndTrustmarkIssuerId(organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark issuer found for id %s".formatted(id)));
    final TrustmarkIssuerDto dto = ModuleToDtoMapper.toDto(entity);
    entity.getEntity().setTrustmarkIssuer(null);
    this.trustmarkIssuerRepository.delete(entity);
    this.auditService.trustmarkIssuerDeleted(id, entity.getEntity().getOrganization().getInstance().getInstanceId(),
        entity.getEntity().getOrganization().getOrganizationId(), dto);
  }

  // ---------------------------------------------------------------------------
  // List Modules
  // ---------------------------------------------------------------------------

  /**
   * Lists all modules for the organization, optionally filtered by type.
   *
   * @param organizationRecord the organization record
   * @param type optional module type filter (trustanchor, intermediate, resolver, trustmarkissuer)
   * @return modules grouped by type
   */
  @Override
  @Transactional(readOnly = true)
  public ModuleDto listModules(final OrganizationRecord organizationRecord, final String type) {
    final FkKeyType moduleType = this.parseModuleType(type);
    final ModuleDto moduleDto = new ModuleDto();

    // Add TrustAnchor modules
    if (moduleType == null || moduleType == FkKeyType.TRUSTANCHOR) {
      final List<TrustAnchorIntermediateModule> trustAnchorModules = this.moduleRepository
          .findByOrgNumberAndOptionalModuleType(organizationRecord.orgNumber(), ModuleType.TRUSTANCHOR);
      final List<TrustAnchorDto> trustAnchors = trustAnchorModules.stream()
          .map(ModuleToDtoMapper::toDto)
          .toList();
      moduleDto.setTrustAnchors(trustAnchors);
    }

    // Add Intermediate modules
    if (moduleType == null || moduleType == FkKeyType.INTERMEDIATE) {
      final List<TrustAnchorIntermediateModule> intermediateModules = this.moduleRepository
          .findByOrgNumberAndOptionalModuleType(organizationRecord.orgNumber(), ModuleType.INTERMEDIATE);
      final List<IntermediateDto> intermediates = intermediateModules.stream()
          .map(ModuleToDtoMapper::toDtoIntermediate)
          .toList();
      moduleDto.setIntermediates(intermediates);
    }

    // Add Resolver modules
    if (moduleType == null || moduleType == FkKeyType.RESOLVER) {
      final List<Resolver> resolverModules = this.resolverRepository
          .findByOrgNumber(organizationRecord.orgNumber());
      final List<ResolverDto> resolvers = resolverModules.stream()
          .map(ModuleToDtoMapper::toDto)
          .toList();
      moduleDto.setResolvers(resolvers);
    }

    // Add TrustmarkIssuer modules
    if (moduleType == null || moduleType == FkKeyType.TRUSTMARKISSUER) {
      final List<TrustMarkIssuer> trustmarkIssuerModules = this.trustmarkIssuerRepository
          .findByOrgNumber(organizationRecord.orgNumber());
      final List<TrustmarkIssuerDto> trustmarkIssuers = trustmarkIssuerModules.stream()
          .map(ModuleToDtoMapper::toDto)
          .toList();
      moduleDto.setTrustmarkIssuers(trustmarkIssuers);
    }

    return moduleDto;
  }

  /**
   * Lists all trustmarks for the organization, optionally including trustmark subjects.
   *
   * @param organizationRecord the organization record
   * @param includeSubjects if true, includes trustmark subjects in the response
   * @return list of trustmarks with optionally included trustmark subjects
   */
  @Override
  @Transactional(readOnly = true)
  public List<TrustmarkWithSubjectsDto> listTrustmarks(final OrganizationRecord organizationRecord,
      final UUID trustmarkIssuerId, final boolean includeSubjects) {
    final List<TrustMark> trustMarkEntities;

    if (includeSubjects) {
      // Fetch trustmarks with subjects using FETCH JOIN
      trustMarkEntities = this.trustMarkRepository
          .findByOrgNumberWithSubjects(organizationRecord.orgNumber(), trustmarkIssuerId);
    }
    else {
      // Fetch trustmarks without subjects
      trustMarkEntities = this.trustMarkRepository
          .findByOrgNumber(organizationRecord.orgNumber(), trustmarkIssuerId);
    }

    // Convert to DTOs - always use TrustmarkWithSubjectsDto, but only populate subjects if requested
    return trustMarkEntities.stream()
        .map(entity -> includeSubjects
            ? TrustmarkToDtoMapper.toDtoWithSubjects(entity)
            : TrustmarkToDtoMapper.toDtoWithSubjectsEmpty(entity))
        .toList();
  }

  /**
   * Get trust mark subjects by there trustmarkid
   *
   * @param organizationRecord the organization record
   * @param trustmarkId the trust mark ID
   * @return the trust mark
   */
  @Override
  @Transactional(readOnly = true)
  public TrustmarkWithSubjectsDto getTrustmarkWithSubjects(final OrganizationRecord organizationRecord,
      final UUID trustmarkId) {
    final TrustMark entity = this.trustMarkRepository
        .findByOrgNumberAndTrustmarkId(organizationRecord.orgNumber(), trustmarkId)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No trust mark found for id %s".formatted(trustmarkId)));

    return TrustmarkToDtoMapper.toDtoWithSubjects(entity);
  }


  private FkKeyType parseModuleType(final String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    try {
      return FkKeyType.valueOf(type.toUpperCase());
    }
    catch (final IllegalArgumentException e) {
      throw new RegistryServerException(ErrorTypes.INVALID_PARAMETER,
          "Invalid module type: %s. Valid values are: trustanchor, intermediate, resolver, trustmarkissuer"
              .formatted(type));
    }
  }
}
