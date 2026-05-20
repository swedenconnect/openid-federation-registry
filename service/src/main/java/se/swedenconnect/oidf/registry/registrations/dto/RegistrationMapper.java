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

import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.registrationflow.dto.Technology;
import se.swedenconnect.oidf.registry.registrationflow.model.FlowAssignment;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessReport;
import se.swedenconnect.oidf.registry.registrationflow.process.StepExecutionRecord;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrations.model.Registration;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.model.TrustmarkSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
   * @param registration the registration
   * @param report process report from the pipeline run
   * @return mapped DTO
   */
  public static RegistrationDto toRegistrationRequestStatusDto(
      final Registration registration, final ProcessReport report) {
    final RegistrationDto dto = toRegistrationDto(registration);
    dto.setSuccessful(report.isSuccessful());
    dto.setSteps(report.steps().stream()
        .map(RegistrationMapper::toStepExecutionRecordDto)
        .toList());
    return dto;
  }

  /**
   * Maps a {@link Registration} entity to a {@link RegistrationDto}.
   *
   * @param model registration entity
   * @return mapped DTO
   */
  public static RegistrationDto toRegistrationDto(final Registration model) {
    final RegistrationDto dto = new RegistrationDto();
    dto.setRegistrationId(model.getRegistrationId());
    dto.setJoinId(model.getFlowAssignment().getAssignId());
    dto.setEntityIdentifier(model.getEntityId());
    dto.setIntermediateEntityId(model.getFlowAssignment().getTaIm().getEntity().getSubject());
    dto.setStatusFedreg(FedRegStatus.valueOf(model.getStatus().toString()));
    dto.setRejectionReason(model.getRejectionReason());
    dto.setStatusTrustmarks(toTrustmarkDtoList(model.getTrustmarksRequested()));
    dto.setOrganizationName(
        Optional.ofNullable(model.getOrganization()).map(Organization::getOrgName).orElse(null));

    final Technology technology = model.getFlowAssignment().getRegistrationFlow().getTechnology();
    final List<RegistrationTagsDto> registrationTags = new ArrayList<>();
    switch (technology) {
    case OIDC -> registrationTags.add(RegistrationTagsDto.OIDC);
    case SAML -> registrationTags.add(RegistrationTagsDto.SAML);
    }

    final String entityType = model.getFlowAssignment().getRegistrationFlow().getEntityType();
    Optional.ofNullable(entityType).ifPresent(s -> registrationTags.add(fromEntityType(s)));

    dto.setTags(registrationTags);
    return dto;
  }

  private static RegistrationTagsDto fromEntityType(final String entityType) {
    return switch (entityType) {
      case "federation_entity" -> RegistrationTagsDto.FED;
      case "openid_relying_party" -> RegistrationTagsDto.RP;
      case "openid_provider" -> RegistrationTagsDto.OP;
      case "oauth_authorization_server" -> RegistrationTagsDto.AS;
      case "oauth_client" -> RegistrationTagsDto.OAC;
      case "oauth_protected_resource" -> RegistrationTagsDto.ORS;
      default -> throw new IllegalArgumentException("Unknown entity_type: " + entityType);
    };
  }
  private static List<TrustmarkRegistrationDto> toTrustmarkDtoList(final List<TrustmarkSource> tmSource) {
    if (tmSource == null) {
      return null;
    }
    return tmSource.stream().map(tm -> {
      final TrustmarkRegistrationDto dto = new TrustmarkRegistrationDto();
      dto.setTrustmarkIssuer(tm.trustMarkIssuer());
      dto.setTrustmarkStatus(Optional.ofNullable(tm.trustmarks())
          .orElse(Collections.emptyList())
          .stream()
          .map(trustMarkStatus ->
              new TrustmarkStatusDto(trustMarkStatus.trustmarkType(),
                  FedRegStatus.valueOf(trustMarkStatus.trustmarkStatus().toString())))
          .toList());
      return dto;
    }).toList();
  }

  /**
   * Maps a list of {@link TrustmarkRegistrationDto} to a list of {@link TrustmarkSource} domain records.
   *
   * @param dtos the source DTOs, may be {@code null}
   * @return mapped domain records, or {@code null} if {@code dtos} is {@code null}
   */
  public static List<TrustmarkSource> toTrustmarkSourceList(final List<TrustmarkRequestDto> dtos) {
    if (dtos == null) {
      return null;
    }
    return dtos.stream()
        .map(dto -> new TrustmarkSource(dto.getTrustmarkIssuer(),
            dto.getTrustmarkType()
                .stream()
                .map(tmType ->
                    new TrustmarkSource.TrustMarkStatus(tmType, RegistrationStatus.STARTED))
                .toList()))
        .toList();
  }

  /**
   * Maps a {@link FlowAssignment} entity to a {@link RegistrationFlowInformationDto}.
   *
   * @param flowAssignment flow assignment entity
   * @return mapped DTO
   */
  public static RegistrationFlowInformationDto toRegistrationFlowDto(final FlowAssignment flowAssignment) {
    final RegistrationFlowInformationDto dto = new RegistrationFlowInformationDto();
    dto.setJoinId(flowAssignment.getAssignId());
    dto.setName(flowAssignment.getRegistrationFlow().getName());
    dto.setDescription(flowAssignment.getRegistrationFlow().getDescription());
    dto.setDescriptionSv(flowAssignment.getRegistrationFlow().getDescriptionSv());
    dto.setTechnology(flowAssignment.getRegistrationFlow().getTechnology().name());
    dto.setEntityType(flowAssignment.getRegistrationFlow().getEntityType());
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
