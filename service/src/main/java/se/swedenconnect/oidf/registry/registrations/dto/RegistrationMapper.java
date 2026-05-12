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
package se.swedenconnect.oidf.registry.registrations.dto;

import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.process.StepExecutionRecord;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;

import java.util.List;
import java.util.UUID;

/**
 * Maps domain objects to registration DTOs.
 *
 * @author Per Fredrik Plars
 */
public final class RegistrationMapper {

  private RegistrationMapper() {
  }

  /**
   * Maps a {@link ProcessReport} and entity identifier to a {@link RegistrationRequestStatusDto}.
   *
   * @param entityIdentifyer entity identifier from the request
   * @param registrationId the registration ID, may be {@code null} if not yet persisted
   * @param report process report from the pipeline run
   * @return mapped DTO
   */
  public static RegistrationRequestStatusDto toRegistrationRequestStatusDto(
      final String entityIdentifyer, final UUID registrationId, final ProcessReport report) {
    final RegistrationRequestStatusDto dto = new RegistrationRequestStatusDto();
    dto.setRegistrationId(registrationId);
    dto.setEntityIdentifyer(entityIdentifyer);
    dto.setStatus(report.status().toString());
    dto.setSuccessful(report.isSuccessful());
    dto.setSteps(report.steps().stream()
        .map(RegistrationMapper::toStepExecutionRecordDto)
        .toList());
    return dto;
  }

  /**
   * Maps a {@link Registration} entity to a {@link RegistrationDto}.
   *
   * @param reg registration entity
   * @return mapped DTO
   */
  public static RegistrationDto toRegistrationDto(final Registration reg) {
    final RegistrationDto dto = new RegistrationDto();
    dto.setRegistrationId(reg.getRegistrationId());
    dto.setJoinId(reg.getFlowAssignment().getAssignId());
    dto.setEntityIdentifyer(reg.getEntityId());
    dto.setIntermediateEntityId(reg.getFlowAssignment().getTaIm().getEntity().getSubject());
    dto.setStatusFedreg(FedRegStatus.valueOf(reg.getStatus().toString()));
    dto.setRejectionReason(reg.getRejectionReason());
    dto.setJwks(reg.getJwks());
    dto.setMetadataPolicy(reg.getMetadataPolicy());
    dto.setTrustmarksRequested(toTrustmarkDtoList(reg.getTrustmarksRequested()));
    return dto;
  }

  private static List<TrustmarkDto> toTrustmarkDtoList(final List<TrustmarkSource> sources) {
    if (sources == null) {
      return null;
    }
    return sources.stream().map(source -> {
      final TrustmarkDto dto = new TrustmarkDto();
      dto.setTrustmarkIssuer(source.trustMarkIssuer());
      dto.setTrustmarkType(source.trustmarkType());
      return dto;
    }).toList();
  }

  /**
   * Maps a list of {@link TrustmarkDto} to a list of {@link TrustmarkSource} domain records.
   *
   * @param dtos the source DTOs, may be {@code null}
   * @return mapped domain records, or {@code null} if {@code dtos} is {@code null}
   */
  public static List<TrustmarkSource> toTrustmarkSourceList(final List<TrustmarkDto> dtos) {
    if (dtos == null) {
      return null;
    }
    return dtos.stream()
        .map(dto -> new TrustmarkSource(dto.getTrustmarkIssuer(), dto.getTrustmarkType()))
        .toList();
  }

  /**
   * Maps a {@link FlowAssignment} entity to a {@link RegistrationFlowDto}.
   *
   * @param flowAssignment flow assignment entity
   * @return mapped DTO
   */
  public static RegistrationFlowDto toRegistrationFlowDto(final FlowAssignment flowAssignment) {
    final RegistrationFlowDto dto = new RegistrationFlowDto();
    dto.setJoinId(flowAssignment.getAssignId());
    dto.setName(flowAssignment.getRegistrationFlow().getName());
    dto.setDescription(flowAssignment.getRegistrationFlow().getDescription());
    dto.setIntermediateEntityId(flowAssignment.getTaIm().getEntity().getSubject());
    return dto;
  }

  private static StepExecutionRecordDto toStepExecutionRecordDto(final StepExecutionRecord record) {
    return new StepExecutionRecordDto(
        record.stepName(),
        record.result().status().toString(),
        record.result().message(),
        record.result().issues().stream()
            .map(RegistrationMapper::toStepIssueDto)
            .toList()
    );
  }

  private static StepIssueDto toStepIssueDto(final StepIssue issue) {
    return new StepIssueDto(issue.field(), issue.message(), issue.severity().toString());
  }
}
