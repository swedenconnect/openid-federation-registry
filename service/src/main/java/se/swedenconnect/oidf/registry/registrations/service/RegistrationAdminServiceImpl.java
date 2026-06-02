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
package se.swedenconnect.oidf.registry.registrations.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.entity.dto.HostedEntityDto;
import se.swedenconnect.oidf.registry.entity.service.EntityConfigService;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.registrationflow.RegistrationFlowService;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationMapper;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link RegistrationAdminService}.
 *
 * @author Per Fredrik Plars
 * @author Felix Hellman
 */
@Service
public class RegistrationAdminServiceImpl implements RegistrationAdminService {

  private final RegistrationRepository registrationRepository;
  private final EntityConfigService entityConfigService;
  private final RegistrationFlowService registrationFlowService;

  /**
   * Constructor.
   *
   * @param registrationRepository repository for registration records
   * @param entityConfigService service for checking hosted entities
   * @param registrationFlowService service for resuming pipeline execution on step approval
   */
  public RegistrationAdminServiceImpl(final RegistrationRepository registrationRepository,
      final EntityConfigService entityConfigService,
      final RegistrationFlowService registrationFlowService) {
    this.registrationRepository = registrationRepository;
    this.entityConfigService = entityConfigService;
    this.registrationFlowService = registrationFlowService;
  }

  @Override
  public long countPending(final UUID taimId) {
    return this.registrationRepository
        .countByFlowAssignment_TaIm_TaImIdAndStatus(taimId, RegistrationStatus.PENDING_APPROVAL);
  }

  @Override
  @Transactional
  public RegistrationDto reject(final UUID registrationId, final String rejectionReason) {
    final Registration reg = this.registrationRepository.findById(registrationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
    if (reg.getStatus() != RegistrationStatus.PENDING_APPROVAL) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Registration %s is not pending approval".formatted(registrationId));
    }
    reg.setStatus(RegistrationStatus.REJECTED);
    reg.setRejectionReason(rejectionReason);
    reg.setReviewedAt(LocalDateTime.now());
    this.registrationRepository.save(reg);
    final List<HostedEntityDto> hostedEntities = this.entityConfigService.listHostedEntity(reg.getEntityId());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    return RegistrationMapper.toRegistrationDto(reg, isHosted, hostedMetadata);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationDto> listRegistrationsConnectedToThisOrgIM(final OrganizationRecord organizationRecord) {
    //TODO this is not the right way to handle organizations
    final Map<String, Map<String, Object>> hostedMetadataByEntityId = new HashMap<>();
    this.entityConfigService.listHostedEntity((String) null)
        .forEach(h -> hostedMetadataByEntityId.put(h.getEntityIdentifier(), h.getMetadata()));
    return this.registrationRepository.findAll().stream()
        .filter(reg -> reg.getFlowAssignment().getTaIm().getOrganization().getOrgNumber()
            .equals(organizationRecord.orgNumber()))
        .map(r -> RegistrationMapper.toRegistrationDto(r,
            hostedMetadataByEntityId.containsKey(r.getEntityId()),
            hostedMetadataByEntityId.get(r.getEntityId())))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RegistrationDto getRegistrationById(final UUID registrationId) {
    final Registration reg = this.registrationRepository.findById(registrationId)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(registrationId)));
    final List<HostedEntityDto> hostedEntities = this.entityConfigService.listHostedEntity(reg.getEntityId());
    final boolean isHosted = !hostedEntities.isEmpty();
    final Map<String, Object> hostedMetadata = isHosted ? hostedEntities.getFirst().getMetadata() : null;
    return RegistrationMapper.toRegistrationDto(reg, isHosted, hostedMetadata);
  }

  @Override
  @Transactional
  public RegistrationDto approveStep(final UUID registrationId, final int stepIndex) {
    this.registrationFlowService.approveStep(registrationId, stepIndex);
    return this.getRegistrationById(registrationId);
  }
}
