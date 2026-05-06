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

import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.guioperations.OidfServiceIntegration;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.util.UUID;

/**
 * Validates RP metadata against a configured validation profile.
 *
 * @author Per Fredrik Plars
 */
@Component
public class LoadEntityConfigurationStep extends NoConfigStepAdapter {

  OidfServiceIntegration oidfServiceIntegration;

  /**
   *
   * @param oidfServiceIntegration
   */
  public LoadEntityConfigurationStep(final OidfServiceIntegration oidfServiceIntegration) {
    super();
    this.oidfServiceIntegration = oidfServiceIntegration;
  }

  @Override
  public String getDescription() {
    return "From a entityid entityconfiguration is loaded. Signature is validated";
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final EntityID entityId = ctx.getRequired(ContextKey.ENTITY_ID);
    final EntityStatement entityStatement = this.oidfServiceIntegration.entityConfigurationOnStandardLocation(entityId);

    final EntityStatementClaimsSet entityStatementClaimsSet = entityStatement.getClaimsSet();
    ctx.put(ContextKey.ENTITY_CONFIGURATION_METADATA, "METADATA");
    ctx.put(ContextKey.ENTITY_CONFIGURATION_JWKS, "JWKS");

    return StepResult.success();
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("A00BCEAD-ECD9-4EB4-8A7B-481D928B2CC9");
  }

}
