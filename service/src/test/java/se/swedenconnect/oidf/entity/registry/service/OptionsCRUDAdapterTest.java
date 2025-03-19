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

package se.swedenconnect.oidf.entity.registry.service;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
class OptionsCRUDAdapterTest {

  @Test
  void createAndValidateInputData() {

    // Arrange
    OptionsCRUDAdapter adapter = new OptionsCRUDAdapter(null, null) {
      @Override
      public boolean supports(final FkKeyType fkKeyType) {
        return false;
      }

      @Override
      public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
        return null;
      }

      @Override
      public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
        return null;
      }

      @Override
      public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
        return null;
      }

      @Override
      public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
        return null;
      }
    };

    final SettingsEntity template = new SettingsEntity();
    template.setKey("id");
    template.setValidation("uuid");
    template.setValueDataType("text");

    final Values existingUserInput = Values.builder()
        .key("id")
        .validation("NonExisting")
        .valueType("nonExisting")
        .build();

    final Values unknownUserInput = Values.builder()
        .key("keyNotFoundInTemplate")
        .validation("NonExisting")
        .valueType("nonExisting")
        .build();

    final List<SettingsEntity> result =
        adapter.createAndValidateInputData(List.of(template), List.of(existingUserInput, unknownUserInput));
    assertEquals(1, result.size());
    assertEquals("id", result.get(0).getKey());
    assertNull(result.get(0).getValidation());
    assertEquals("text", result.get(0).getValueDataType());

  }
}