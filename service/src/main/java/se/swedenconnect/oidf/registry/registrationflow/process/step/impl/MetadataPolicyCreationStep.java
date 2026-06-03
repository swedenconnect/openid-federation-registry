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

import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;

import java.util.List;
import java.util.UUID;

/**
 * Validates RP metadata against a configured validation profile.
 *
 * @author Per Fredrik Plars
 */
@Component
public class MetadataPolicyCreationStep extends NoConfigStepAdapter {


  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final JSONObject o = ctx.getRequired(ContextKey.ENTITY_CONFIGURATION_METADATA, JSONObject.class);

    final MetadataPolicy metadataPolicy = new MetadataPolicy();
    //TODO generate a policy according to a pre specified template
    ctx.put(ContextKey.METADATA_POLICY, metadataPolicy.toJSONObject());

    return StepResult.success();
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("FCF26DE1-93BF-4B80-A5E5-F4C88BDFFFEA");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(new StepConfigurationValue("manualreview",
        StepConfigurationValue.DATA_TYPE.BOOLEAN,
        "If true, registration requires manual approval before proceeding", "false"));
  }
}
