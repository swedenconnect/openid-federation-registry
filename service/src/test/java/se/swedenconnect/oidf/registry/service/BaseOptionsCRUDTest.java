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

package se.swedenconnect.oidf.registry.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.fixture.JwtTestUtils;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.swedenconnect.oidf.registry.fixture.JwtTestUtils.createOrganizationRecord;

/**
 * Unit tests for {@link BaseOptionsCRUD}
 *
 * @author Per Fredrik Plars
 */
class BaseOptionsCRUDTest {

  @Test
  @DisplayName("Base Options Create And Validate InputData - should succeed")
  void createAndValidateInputData() {

    // Arrange
    BaseOptionsCRUD adapter = new BaseOptionsCRUD(null, null) {
      @Override
      public boolean supports(final FkKeyType fkKeyType) {
        return false;
      }

      @Override
      public OptionsRecord create(final OrganizationRecord organizationRecord,
          final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
        return null;
      }

      @Override
      public OptionsRecord update(final OrganizationRecord organizationRecord,
          final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
        return null;
      }

      @Override
      public OptionsRecord get(final OrganizationRecord organizationRecord,
          final FkKeyType fkKeyType, final UUID id) {
        return null;
      }

      @Override
      public OptionsRecord delete(final OrganizationRecord organizationRecord,
          final FkKeyType fkKeyType, final UUID id) {
        return null;
      }
    };

    final SettingsEntity iss = SettingsEntity.builder().key("issuer")
        .validation("URL").valueDataType("TEXT").build();

    final SettingsEntity sub = SettingsEntity.builder().key("subject")
        .validation("URL").valueDataType("TEXT").build();

    final SettingsEntity entityID = SettingsEntity.builder().key("entity_id")
        .validation("UUID").valueDataType("TEXT").build();

    final SettingsEntity requiredValue = SettingsEntity.builder().key("reg_id")
        .validation("required").valueDataType("TEXT").build();

    final Values existingUserInput = Values.builder()
        .key("subject")
        .validation("NonExisting")
        .valueType("nonExisting")
        .build();

    final Values unknownUserInput = Values.builder()
        .key("keyNotFoundInTemplate")
        .validation("NonExisting")
        .valueType("nonExisting")
        .build();

    assertThatThrownBy(() ->
        adapter.createAndValidateInputData(createOrganizationRecord(JwtTestUtils.OrganisationType.PM),
            List.of(sub, iss, entityID, requiredValue), List.of(
                Values.builder().key("issuer").value("http://issuer").build(),
                Values.builder().key("subject").value("http://subject").build()
            )))
        .isInstanceOf(PropertyValidationFailException.class);

    final List<SettingsEntity> resultReq =
        adapter.createAndValidateInputData(createOrganizationRecord(JwtTestUtils.OrganisationType.PM),
            List.of(sub, iss, entityID, requiredValue), List.of(
            Values.builder().key("issuer").value("http://issuer").build(),
            Values.builder().key("subject").value("http://subject").build(),
            Values.builder().key("reg_id").value("http://subject").build(),
            unknownUserInput
        ));

    assertThat(resultReq).isNotEmpty().hasSize(3);

    final List<SettingsEntity> result =
        adapter.createAndValidateInputData(createOrganizationRecord(JwtTestUtils.OrganisationType.PM),
            List.of(sub, iss, entityID), List.of(existingUserInput, unknownUserInput));

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.getFirst().getKey()).isEqualTo("subject");

    assertThat(result.getFirst().getValidation()).isNull();
    assertThat(result.getFirst().getValueDataType()).isEqualTo("TEXT");

  }
}