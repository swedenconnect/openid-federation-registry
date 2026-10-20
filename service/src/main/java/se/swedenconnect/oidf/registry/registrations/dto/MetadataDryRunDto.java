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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * The outcome of a dry run of a flow's metadata steps: what the subordinate statement would pin and override, and
 * what is in error. Nothing is stored by a dry run.
 *
 * @param pinned the parameters the subordinate statement would pin
 * @param overridden the parameters whose resolved value is not the one the entity declares
 * @param errors the failed checks and policy errors
 * @param failed whether the errors would fail the registration, rather than be shown to a reviewer
 * @param willTriggerReview whether submitting would wait for an operator's approval
 * @param checks every check made, passed or failed
 * @param metadata the {@code metadata} claim that would be published, keyed by entity type
 * @param metadataPolicy the {@code metadata_policy} claim that would be published, keyed by entity type
 * @author Felix Hellman
 */
@Schema(name = "MetadataDryRun")
public record MetadataDryRunDto(

    @Schema(description = "Metadata parameters the subordinate statement would pin: set by its metadata claim or "
        + "fixed by the value operator of its metadata_policy claim")
    List<PinnedParameterDto> pinned,

    @Schema(description = "Metadata parameters whose value, once the subordinate statement's claims are applied, is "
        + "not the one the entity declares")
    List<Overridden> overridden,

    @Schema(description = "Failed checks, metadata policy errors and why the entity's metadata could not be loaded")
    List<String> errors,

    @Schema(description = "True if the errors would decline the registration; false if there are none or a reviewer "
        + "is shown them")
    boolean failed,

    @Schema(description = "True if submitting would wait for an operator's approval; false if it would be published "
        + "directly")
    boolean willTriggerReview,

    @Schema(description = "Every check made, passed or failed")
    List<Check> checks,

    @Schema(description = "The metadata claim the subordinate statement would have, keyed by entity type")
    Map<String, Object> metadata,

    @Schema(description = "The metadata_policy claim the subordinate statement would have, keyed by entity type")
    Map<String, Object> metadataPolicy
) {

  /**
   * A metadata parameter whose resolved value is not the one the entity declares.
   *
   * @param entityType the entity type the parameter belongs to
   * @param parameter the parameter, with language tag if it has one
   * @param entityValue the value the entity declares, {@code null} if none
   * @param value the resolved value, {@code null} if the parameter is removed
   * @param claim the claim that changes it: {@code metadata} or {@code metadata_policy}
   * @param operators the {@code metadata_policy} operators on the parameter
   */
  @Schema(name = "MetadataOverridden")
  public record Overridden(

      @Schema(description = "Entity type the parameter belongs to", example = "openid_relying_party")
      String entityType,

      @Schema(description = "Metadata parameter, with language tag if it has one", example = "client_name")
      String parameter,

      @Schema(description = "The value the entity declares; absent if it declares none")
      Object entityValue,

      @Schema(description = "The value the federation resolves; absent if the parameter is removed")
      Object value,

      @Schema(description = "Claim of the subordinate statement that changes the value: metadata or metadata_policy",
          example = "metadata_policy")
      String claim,

      @Schema(description = "Operators of the metadata_policy claim on the parameter; empty for the metadata claim",
          example = "[\"value\"]")
      List<String> operators
  ) {
  }

  /**
   * A check made on the entity's metadata.
   *
   * @param status {@code OK}, {@code FAIL} or {@code INFO}
   * @param message what was checked
   */
  @Schema(name = "MetadataCheck")
  public record Check(

      @Schema(description = "OK, FAIL or INFO", example = "OK")
      String status,

      @Schema(description = "What was checked", example = "logo_uri is an HTTPS URL")
      String message
  ) {
  }
}
