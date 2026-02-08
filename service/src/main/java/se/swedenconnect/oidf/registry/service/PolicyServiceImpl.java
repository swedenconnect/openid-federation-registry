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

package se.swedenconnect.oidf.registry.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.DtoToEntityMapper;
import se.swedenconnect.oidf.registry.dto.EntityToDtoMapper;
import se.swedenconnect.oidf.registry.dto.PolicyDto;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.validation.ValidateDto;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link PolicyService}.
 *
 * @author Per Fredrik Plars
 */
@Service
public class PolicyServiceImpl implements PolicyService {

  private final PolicyRepository policyRepository;
  private final OrganizationService organizationService;
  private final RegistryAuditService auditService;

  /**
   * Constructor.
   *
   * @param policyRepository the policy repository
   * @param organizationService the organization service
   * @param auditService the audit service
   */
  public PolicyServiceImpl(final PolicyRepository policyRepository,
      final OrganizationService organizationService,
      final RegistryAuditService auditService) {
    this.policyRepository = policyRepository;
    this.organizationService = organizationService;
    this.auditService = auditService;
  }

  private OrganizationEntity resolveOrganization(final OrganizationRecord organizationRecord) {
    return this.organizationService.findCreate(
        organizationRecord.orgNumber(), organizationRecord.orgName());
  }

  private PolicyEntity findPolicyOrThrow(final OrganizationRecord organizationRecord, final UUID id) {
    return this.policyRepository.findByOrgNumberAndPolicyId(
            organizationRecord.orgNumber(), id)
        .orElseThrow(() -> new RegistryServerException(
            ErrorTypes.NOT_FOUND, "No policy found for id %s".formatted(id)));
  }

  private static PolicyDto toDto(final PolicyEntity entity) {
    return EntityToDtoMapper.toDto(entity);
  }

  /**
   * Lists all policies.
   *
   * @param organizationRecord the organization record
   * @return list of policies
   */
  @Override
  @Transactional(readOnly = true)
  public List<PolicyDto> listPolicies(final OrganizationRecord organizationRecord) {
    return this.policyRepository.findByOrgNumber(organizationRecord.orgNumber())
        .stream()
        .map(PolicyServiceImpl::toDto)
        .toList();
  }

  /**
   * Gets a policy by ID.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @return the policy
   */
  @Override
  @Transactional(readOnly = true)
  public PolicyDto getPolicy(final OrganizationRecord organizationRecord, final UUID id) {
    final PolicyEntity entity = this.findPolicyOrThrow(organizationRecord, id);
    return toDto(entity);
  }

  /**
   * Creates a policy.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @param input the policy data
   * @return the created policy
   */
  @Override
  @Transactional
  public PolicyDto createPolicy(final OrganizationRecord organizationRecord,
      final UUID id, final PolicyDto input) {
    ValidateDto.init(organizationRecord).validate(input);
    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final PolicyEntity entity = DtoToEntityMapper.toEntity(id, input, org);
    this.policyRepository.save(entity);
    final PolicyDto dto = toDto(entity);
    this.auditService.policyCreated(id, org.getOrganizationId(), null, dto);
    return dto;
  }

  /**
   * Updates a policy.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @param input the policy data
   * @return the updated policy
   */
  @Override
  @Transactional
  public PolicyDto updatePolicy(final OrganizationRecord organizationRecord,
      final UUID id, final PolicyDto input) {
    ValidateDto.init(organizationRecord).validate(input);
    final PolicyEntity existing = this.findPolicyOrThrow(organizationRecord, id);
    final PolicyDto oldDto = toDto(existing);

    DtoToEntityMapper.updateEntity(existing, input);

    this.policyRepository.save(existing);
    final PolicyDto newDto = toDto(existing);
    this.auditService.policyUpdated(id, existing.getOrganizationId(), oldDto, newDto);
    return newDto;
  }

  /**
   * Deletes a policy.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   */
  @Override
  @Transactional
  public void deletePolicy(final OrganizationRecord organizationRecord, final UUID id) {
    final PolicyEntity entity = this.findPolicyOrThrow(organizationRecord, id);
    final PolicyDto dto = toDto(entity);
    this.policyRepository.delete(entity);
    this.auditService.policyDeleted(id, entity.getOrganizationId(), dto);
  }

  /**
   * Gets a policy DTO.
   *
   * @param organizationRecord the organization record
   * @param id the policy ID
   * @return the policy DTO
   */
  @Override
  @Transactional(readOnly = true)
  public PolicyDto getPolicyDomain(final OrganizationRecord organizationRecord, final UUID id) {
    final PolicyEntity entity = this.findPolicyOrThrow(organizationRecord, id);
    return toDto(entity);
  }
}


