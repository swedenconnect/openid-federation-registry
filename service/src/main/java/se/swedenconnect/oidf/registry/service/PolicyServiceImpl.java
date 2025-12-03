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
import se.swedenconnect.oidf.registry.api.dto.PolicyDto;
import se.swedenconnect.oidf.registry.api.dto.input.PolicyInputDto;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.domain.EntityToDomain;
import se.swedenconnect.oidf.registry.domain.Policies;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.registry.errorhandling.ErrorTypes;
import se.swedenconnect.oidf.registry.errorhandling.RegistryServerException;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;

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

  public PolicyServiceImpl(final PolicyRepository policyRepository,
      final OrganizationService organizationService) {
    this.policyRepository = policyRepository;
    this.organizationService = organizationService;
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
    final Policies policies = EntityToDomain.map(entity);
    final PolicyDto dto = new PolicyDto();
    dto.setPolicyId(policies.getPolicyId());
    dto.setName(policies.getName());
    dto.setPolicy(policies.getPolicy());
    return dto;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PolicyDto> listPolicies(final OrganizationRecord organizationRecord) {
    return this.policyRepository.findByOrgNumber(organizationRecord.orgNumber())
        .stream()
        .map(PolicyServiceImpl::toDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PolicyDto getPolicy(final OrganizationRecord organizationRecord, final UUID id) {
    final PolicyEntity entity = this.findPolicyOrThrow(organizationRecord, id);
    return toDto(entity);
  }

  @Override
  @Transactional
  public PolicyDto createPolicy(final OrganizationRecord organizationRecord,
      final UUID id, final PolicyInputDto input) {
    final OrganizationEntity org = this.resolveOrganization(organizationRecord);
    final Policies policies = EntityToDomain.toDomain(input);
    final PolicyEntity entity = EntityToDomain.toPolicyEntity(id, policies, org);
    this.policyRepository.save(entity);
    return toDto(entity);
  }

  @Override
  @Transactional
  public PolicyDto updatePolicy(final OrganizationRecord organizationRecord,
      final UUID id, final PolicyInputDto input) {
    final Policies policies = EntityToDomain.toDomain(input);
    final PolicyEntity existing = this.findPolicyOrThrow(organizationRecord, id);

    final PolicyEntity updated = EntityToDomain.toPolicyEntity(
        id, policies, existing.getOrganization());

    this.policyRepository.save(updated);
    return toDto(updated);
  }

  @Override
  @Transactional
  public void deletePolicy(final OrganizationRecord organizationRecord, final UUID id) {
    final PolicyEntity entity = this.findPolicyOrThrow(organizationRecord, id);
    this.policyRepository.delete(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Policies getPolicyDomain(final OrganizationRecord organizationRecord, final UUID id) {
    final PolicyEntity entity = this.findPolicyOrThrow(organizationRecord, id);
    return EntityToDomain.map(entity);
  }
}


