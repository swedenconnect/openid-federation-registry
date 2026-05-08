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
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;
import se.swedenconnect.oidf.registry.registrations.dto.FedRegStatus;
import se.swedenconnect.oidf.registry.registrations.dto.RegistrationDto;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link RegistrationAdminService}.
 *
 * @author Per Fredrik Plars
 */
@Service
public class RegistrationAdminServiceImpl implements RegistrationAdminService {

  private final RegistrationRepository registrationRepository;

  /**
   * Constructor.
   *
   * @param registrationRepository repository for registration records
   */
  public RegistrationAdminServiceImpl(final RegistrationRepository registrationRepository) {
    this.registrationRepository = registrationRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RegistrationDto> listAll(final OrganizationRecord organizationRecord) {
    return this.registrationRepository
        .findAllByOrganizationOrgNumber(organizationRecord.orgNumber())
        .stream()
        .map(RegistrationAdminServiceImpl::toDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RegistrationDto getById(final UUID id) {
    return this.registrationRepository.findByIdFetched(id)
        .map(RegistrationAdminServiceImpl::toDto)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(id)));
  }

  @Override
  public long countPending(final UUID taimId) {
    return this.registrationRepository
        .countByFlowAssignment_TaIm_TaImIdAndStatus(taimId, RegistrationStatus.PENDING_APPROVAL);
  }

  @Override
  @Transactional
  public RegistrationDto reject(final UUID id, final String rejectionReason) {
    final Registration reg = this.registrationRepository.findByIdFetched(id)
        .orElseThrow(() -> new RegistryServerException(ErrorTypes.NOT_FOUND,
            "Registration not found: %s".formatted(id)));
    if (reg.getStatus() != RegistrationStatus.PENDING_APPROVAL) {
      throw new RegistryServerException(ErrorTypes.CONFLICT,
          "Registration %s is not pending approval".formatted(id));
    }
    reg.setStatus(RegistrationStatus.REJECTED);
    reg.setRejectionReason(rejectionReason);
    reg.setReviewedAt(LocalDateTime.now());
    this.registrationRepository.save(reg);
    return toDto(reg);
  }

  private static RegistrationDto toDto(final Registration reg) {
    final RegistrationDto dto = new RegistrationDto();
    dto.setRegistrationId(reg.getRegistrationId());
    dto.setJoinId(reg.getFlowAssignment().getAssignId());
    dto.setEntityId(reg.getEntityId());
    dto.setIntermediateEntityId(reg.getFlowAssignment().getTaIm().getEntity().getSubject());
    dto.setStatusFedreg(FedRegStatus.valueOf(reg.getStatus().name()));
    dto.setRejectionReason(reg.getRejectionReason());
    dto.setJwks(reg.getJwks());
    dto.setMetadataPolicy(reg.getMetadataPolicy());
    dto.setTrustmarksRequested(reg.getTrustmarksRequested());
    return dto;
  }
}
