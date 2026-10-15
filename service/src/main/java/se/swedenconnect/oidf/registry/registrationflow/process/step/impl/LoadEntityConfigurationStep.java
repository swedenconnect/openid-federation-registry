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
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import se.swedenconnect.oidf.registry.guioperations.OidfService;
import se.swedenconnect.oidf.registry.infrastructure.validation.CleanInput;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Loading entityconfiguration and jwks. The entity configuration is loaded in {@code buildContext}, so that an
 * operator reviewing this step sees what was loaded before anything downstream uses it.
 *
 * @author Per Fredrik Plars
 */
@Component
public class LoadEntityConfigurationStep extends NoConfigStepAdapter implements ReviewableStep {

  final OidfService oidfService;

  /**
   * Constructor
   * @param oidfService oidfService used to load entitystatements
   */
  public LoadEntityConfigurationStep(final OidfService oidfService) {
    super();
    this.oidfService = oidfService;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public String getDescription() {
    return "From a entityid entityconfiguration is loaded. Signature is validated";
  }

  @Override
  public boolean canApply(final ProcessContext ctx, final StepConfig config) {
    return !Step.isHosted(ctx);
  }

  @Override
  public StepResult buildContext(final ProcessContext ctx, final StepConfig config) {
    return this.loadEntityConfiguration(ctx);
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    if (ctx.get(ContextKey.ENTITY_CONFIGURATION_METADATA).isEmpty()
        || ctx.get(ContextKey.ENTITY_CONFIGURATION_JWKS).isEmpty()) {
      return this.loadEntityConfiguration(ctx);
    }
    return StepResult.success("EntityConfiguration loaded successfully");
  }

  /**
   * Loads the entity configuration. A fetch that fails (the entity cannot be reached, or answers with an error) is a
   * technical issue; a configuration that is fetched but not valid is a validation issue the entity can fix.
   */
  private StepResult loadEntityConfiguration(final ProcessContext ctx) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final EntityStatement entityStatement;
    try {
      entityStatement = this.oidfService.loadEntityStatement(new EntityID(entityId));
    }
    catch (final RestClientException | SecurityException e) {
      // SecurityException: the outgoing request was refused before it was sent (unknown host, blocked address).
      return StepResult.failure("The entity configuration could not be fetched", List.of(StepIssue.technical(
          "LoadEntityConfigurationStep.fetch", "Could not fetch the entity configuration of %s: %s%s"
              .formatted(entityId, e.getMessage(),
                  Optional.ofNullable(e.getCause()).map(cause -> " (" + cause.getMessage() + ")").orElse("")),
          Severity.ERROR)));
    }
    catch (final IllegalArgumentException | IllegalStateException e) {
      return StepResult.failure("The entity configuration is not valid", List.of(new StepIssue(
          "LoadEntityConfigurationStep.entityConfiguration", "The entity configuration is not valid: "
              + Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage()),
          Severity.ERROR)));
    }

    final EntityStatementClaimsSet esClaimsSet = entityStatement.getClaimsSet();
    ctx.put(ContextKey.ENTITY_CONFIGURATION_METADATA, esClaimsSet.getJSONObjectClaim("metadata"));

    ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, CleanInput.removeExpIatNbfFromJwks(esClaimsSet.getJWKSet()));
    return StepResult.success("EntityConfiguration loaded successfully");
  }

  @Override
  public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final JSONObject metadata = ctx.<JSONObject>get(ContextKey.ENTITY_CONFIGURATION_METADATA).orElse(null);
    final List<String> keyIds = ctx.<JWKSet>get(ContextKey.ENTITY_CONFIGURATION_JWKS)
        .map(jwks -> jwks.getKeys().stream().map(JWK::getKeyID).filter(Objects::nonNull).toList())
        .orElse(List.of());
    return StepPreview.builder()
        .add(PreviewSection.checks("Checks", List.of(
            new PreviewSection.Check(PreviewSection.CheckStatus.OK,
                "Entity Configuration fetched and its signature verified"))))
        .add(PreviewSection.fields("Entity Configuration", List.of(
            new PreviewSection.Field("Entity identifier", entityId),
            new PreviewSection.Field("Entity types", metadata == null ? "" : String.join(", ", metadata.keySet())),
            new PreviewSection.Field("Federation key ids", String.join(", ", keyIds)))))
        .build();
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(new StepConfigurationValue(MANUAL_REVIEW,
        StepConfigurationValue.DATA_TYPE.BOOLEAN,
        "If true, registration requires manual approval before proceeding", "false"));
  }
}
