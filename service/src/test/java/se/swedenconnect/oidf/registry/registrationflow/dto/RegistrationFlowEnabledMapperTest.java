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
package se.swedenconnect.oidf.registry.registrationflow.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.registrationflow.model.RegistrationFlow;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Step;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@code enabled} flag of a registration flow. A client that predates the flag sends no value at
 * all, and such a flow must come out usable — anything else would silently take existing flows out of service.
 *
 * <p>The step repository is never consulted for a TRUST_MARK_ISSUER flow with no steps, so {@code null} is passed
 * for it here.</p>
 */
class RegistrationFlowEnabledMapperTest {

  private static RegistrationFlowDto dtoWithEnabled(final Boolean enabled) {
    return new RegistrationFlowDto(UUID.randomUUID(), "Flow", "A flow", null, null, null, List.of(),
        Step.FlowType.TRUST_MARK_ISSUER, enabled);
  }

  @Test
  @DisplayName("A create request that omits enabled produces an enabled flow")
  void absentEnabledDefaultsToTrueOnCreate() {
    final RegistrationFlow flow = Mapper.toModel(dtoWithEnabled(null), UUID.randomUUID(), null, null);

    assertThat(flow.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("An explicit false on create disables the flow")
  void explicitFalseDisablesOnCreate() {
    final RegistrationFlow flow = Mapper.toModel(dtoWithEnabled(false), UUID.randomUUID(), null, null);

    assertThat(flow.isEnabled()).isFalse();
  }

  @Test
  @DisplayName("An update carries the flag onto the stored flow, both ways")
  void updateAppliesTheFlagInBothDirections() {
    final RegistrationFlow stored = new RegistrationFlow();
    stored.setEnabled(true);

    assertThat(Mapper.applyUpdate(stored, dtoWithEnabled(false), null).isEnabled()).isFalse();
    assertThat(Mapper.applyUpdate(stored, dtoWithEnabled(true), null).isEnabled()).isTrue();
  }

  @Test
  @DisplayName("An update that omits enabled leaves the flow usable, matching create")
  void absentEnabledDefaultsToTrueOnUpdate() {
    final RegistrationFlow stored = new RegistrationFlow();
    stored.setEnabled(false);

    assertThat(Mapper.applyUpdate(stored, dtoWithEnabled(null), null).isEnabled()).isTrue();
  }

  @Test
  @DisplayName("A freshly constructed flow entity is enabled")
  void newEntityIsEnabled() {
    assertThat(new RegistrationFlow().isEnabled()).isTrue();
  }
}
