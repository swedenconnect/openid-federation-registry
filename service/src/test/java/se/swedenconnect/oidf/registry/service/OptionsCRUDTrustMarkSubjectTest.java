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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.api.model.OptionRecord;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.entity.SettingsEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkEntity;
import se.swedenconnect.oidf.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkRepository;
import se.swedenconnect.oidf.registry.repository.TrustMarkSubjectRepository;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link OptionsCRUDTrustMarkSubject}.
 *
 * @author David Goldring
 */
@ExtendWith(MockitoExtension.class)
class OptionsCRUDTrustMarkSubjectTest {

  @Mock
  private OrganizationService organizationService;
  @Mock
  private SettingsRepository settingsRepository;
  @Mock
  private TrustMarkRepository trustMarkRepository;
  @Mock
  private TrustMarkSubjectRepository trustMarkSubjectRepository;

  private OptionsCRUDTrustMarkSubject service;

  private final String orgNumber = "1234567890";
  private final OrganizationRecord orgRecord = new OrganizationRecord(orgNumber, "Test Org", "USER");
  private final FkKeyType fkKeyType = FkKeyType.TRUSTMARKSUBJECT;

  @BeforeEach
  void setUp() {
    service = new OptionsCRUDTrustMarkSubject(
        organizationService,
        settingsRepository,
        trustMarkRepository,
        trustMarkSubjectRepository
    );
  }

  @Test
  @DisplayName("Update TMS - should succeed")
  void testUpdate_should_succeed() {
    // Setup data
    final UUID trustMarkId = UUID.randomUUID();
    final UUID trustMarkSubjectId = UUID.randomUUID();

    // Mock TrustMarkEntity
    final TrustMarkEntity trustMarkEntity = new TrustMarkEntity();
    trustMarkEntity.setTrustmarkId(trustMarkId);
    trustMarkEntity.setTrustmarksubjects(new ArrayList<>());

    // Mock TrustMarkSubjectEntity
    final TrustMarkSubjectEntity subjectEntity = new TrustMarkSubjectEntity();
    subjectEntity.setTrustmarksubjectId(trustMarkSubjectId);
    subjectEntity.setTrustMark(trustMarkEntity);

    // Mock Settings
    final SettingsEntity templateSetting = createSettingsEntity("some_setting", "default", "TEXT");

    // Mock Input Record
    final OptionsRecord inputRecord = OptionsRecord.builder()
        .option(List.of(
            Values.builder().key("some_setting").value("new_value").build()
        ))
        .build();

    // Mocks behavior
    when(trustMarkSubjectRepository.findByOrgNumberAndTrustmarkId(orgNumber, trustMarkSubjectId))
        .thenReturn(Optional.of(subjectEntity));

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), "TEMPLATE"))
        .thenReturn(List.of(templateSetting));

    // Simulate JPA saving behavior
    mockSettingsRepositorySave();

    // Execute
    service.update(orgRecord, fkKeyType, trustMarkSubjectId, inputRecord);

    // Capture arguments passed to settingsRepository.saveAllAndFlush
    @SuppressWarnings("unchecked")
    final ArgumentCaptor<List<SettingsEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(settingsRepository).saveAllAndFlush(captor.capture());

    final List<SettingsEntity> savedSettings = captor.getValue();

    // Assert
    assertThat(savedSettings).hasSize(1);
    assertThat(savedSettings.getFirst().getFkId()).isEqualTo(trustMarkSubjectId.toString());
    assertThat(savedSettings.getFirst().getValue()).isEqualTo("new_value");

    verify(settingsRepository).findByFkTypeAndFkId(fkKeyType.name(), trustMarkSubjectId.toString());
  }

  @Test
  @DisplayName("Create TMS - should succeed")
  void testCreate_should_succeed() {
    // Setup data
    final UUID trustMarkId = UUID.randomUUID();
    final UUID trustMarkSubjectId = UUID.randomUUID();

    // Mock TrustMarkEntity
    final TrustMarkEntity trustMarkEntity = new TrustMarkEntity();
    trustMarkEntity.setTrustmarkId(trustMarkId);
    trustMarkEntity.setTrustmarksubjects(new ArrayList<>());

    // Mock Settings
    final SettingsEntity templateSetting = createSettingsEntity("trustmark_id", "default", "TEXT");

    // Mock Input Record
    final OptionsRecord inputRecord = OptionsRecord.builder()
        .option(List.of(
            Values.builder().key("trustmark_id").value(trustMarkId.toString()).build()
        ))
        .build();

    // Mocks behavior
    when(trustMarkSubjectRepository.findByOrgNumberAndTrustmarkId(orgNumber, trustMarkSubjectId))
        .thenReturn(Optional.empty());

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), "TEMPLATE"))
        .thenReturn(List.of(templateSetting));

    when(trustMarkRepository.findByOrgNumberAndTrustmarkId(eq(orgNumber), any(UUID.class)))
        .thenReturn(Optional.of(trustMarkEntity));

    when(trustMarkSubjectRepository.saveAndFlush(any(TrustMarkSubjectEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    mockSettingsRepositorySave();

    // Execute
    service.create(orgRecord, fkKeyType, trustMarkSubjectId, inputRecord);

    // Capture arguments
    @SuppressWarnings("unchecked")
    final ArgumentCaptor<List<SettingsEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(settingsRepository).saveAllAndFlush(captor.capture());

    final List<SettingsEntity> savedSettings = captor.getValue();

    // Assert
    assertThat(savedSettings).hasSize(1);
    assertThat(savedSettings.getFirst().getFkId()).isEqualTo(trustMarkSubjectId.toString());
  }

  @Test
  @DisplayName("Get TMS - should succeed")
  void testGet_should_succeed() {
    final UUID trustMarkSubjectId = UUID.randomUUID();

    final TrustMarkSubjectEntity subjectEntity = new TrustMarkSubjectEntity();
    subjectEntity.setTrustmarksubjectId(trustMarkSubjectId);

    // Mock settings
    final SettingsEntity storedSetting = createSettingsEntity("some_setting", "stored_value", "TEXT");
    final SettingsEntity templateSetting = createSettingsEntity("some_setting", "default", "TEXT");

    when(trustMarkSubjectRepository.findByOrgNumberAndTrustmarkId(orgNumber, trustMarkSubjectId))
        .thenReturn(Optional.of(subjectEntity));

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), trustMarkSubjectId.toString()))
        .thenReturn(List.of(storedSetting));

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), "TEMPLATE"))
        .thenReturn(List.of(templateSetting));

    // Execute
    final OptionsRecord result = service.get(orgRecord, fkKeyType, trustMarkSubjectId);

    // Assert
    assertThat(result.getOption()).hasSize(1);
    assertThat(result.getOption().getFirst().getKey()).isEqualTo("some_setting");
    assertThat(result.getOption().getFirst().getValue()).isEqualTo("stored_value");
  }

  @Test
  @DisplayName("Delete TMS - should succeed")
  void testDelete_should_succeed() {
    final UUID trustMarkSubjectId = UUID.randomUUID();
    final UUID trustMarkId = UUID.randomUUID();

    final TrustMarkEntity trustMarkEntity = new TrustMarkEntity();
    trustMarkEntity.setTrustmarkId(trustMarkId);
    trustMarkEntity.setTrustmarksubjects(new ArrayList<>());

    final TrustMarkSubjectEntity subjectEntity = new TrustMarkSubjectEntity();
    subjectEntity.setTrustmarksubjectId(trustMarkSubjectId);
    subjectEntity.setTrustMark(trustMarkEntity);

    final SettingsEntity storedSetting = createSettingsEntity("some_setting", "stored_value", "TEXT");

    when(trustMarkSubjectRepository.findByOrgNumberAndTrustmarkId(orgNumber, trustMarkSubjectId))
        .thenReturn(Optional.of(subjectEntity));

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), trustMarkSubjectId.toString()))
        .thenReturn(List.of(storedSetting));

    // Execute
    service.delete(orgRecord, fkKeyType, trustMarkSubjectId);

    // Assert
    verify(trustMarkSubjectRepository).delete(subjectEntity);
    verify(settingsRepository).deleteAllInBatch(anyList());

    verify(settingsRepository).findByFkTypeAndFkId(fkKeyType.name(), trustMarkSubjectId.toString());
  }

  @Test
  @DisplayName("Template - should return template with options")
  void testTemplate_should_succeed() {
    final UUID trustMarkId = UUID.randomUUID();

    // Mock template setting for trustmark_id which is an OPTIONS type
    final SettingsEntity templateSetting = createSettingsEntity("trustmark_id", "default", "OPTIONS");

    // Mock existing trust mark for options-list
    final TrustMarkEntity trustMarkEntity = new TrustMarkEntity();
    trustMarkEntity.setTrustmarkId(trustMarkId);
    final SettingsEntity tmNameSetting = createSettingsEntity("trust_mark_entity_id", "My Trust Mark", "TEXT");
    trustMarkEntity.setSettingsEntityList(List.of(tmNameSetting));

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), "TEMPLATE"))
        .thenReturn(List.of(templateSetting));

    when(trustMarkRepository.findByOrgNumber(orgNumber))
        .thenReturn(List.of(trustMarkEntity));

    // Execute
    final OptionsRecord result = service.template(orgRecord, fkKeyType);

    // Assert
    assertThat(result.getOption()).hasSize(1);
    final Values option = result.getOption().getFirst();
    assertThat(option.getKey()).isEqualTo("trustmark_id");

    // Check populated options
    assertThat(option.getOptions()).hasSize(1);
    final OptionRecord choice = option.getOptions().getFirst();
    assertThat(choice.getKey()).isEqualTo(trustMarkId.toString());
    assertThat(choice.getValue()).isEqualTo("My Trust Mark");
  }

  @Test
  @DisplayName("List TMS - should return list of entities")
  void testList_should_succeed() {
    final UUID trustMarkSubjectId = UUID.randomUUID();
    final TrustMarkSubjectEntity subjectEntity = new TrustMarkSubjectEntity();
    subjectEntity.setTrustmarksubjectId(trustMarkSubjectId);

    final SettingsEntity setting1 = createSettingsEntity("key1", "val1", "TEXT");

    when(trustMarkSubjectRepository.findByOrgNumber(orgNumber))
        .thenReturn(List.of(subjectEntity));

    when(settingsRepository.findByFkTypeAndFkId(fkKeyType.name(), trustMarkSubjectId.toString()))
        .thenReturn(List.of(setting1));

    // Execute
    final List<Map<String, Object>> result = service.list(orgRecord, fkKeyType);

    // Assert
    assertThat(result).hasSize(1);
    final Map<String, Object> map = result.getFirst();
    assertThat(map).containsEntry("id", trustMarkSubjectId.toString());
    assertThat(map).containsEntry("key1", "val1");
  }

  // Helper to create settings with dates to avoid NPE in base class
  private SettingsEntity createSettingsEntity(String key, String value, String type) {
    SettingsEntity e = new SettingsEntity();
    e.setKey(key);
    e.setValue(value);
    e.setValueDataType(type);
    e.setValidation(null);
    e.setCreatedDate(LocalDateTime.now());
    e.setLastModifiedDate(LocalDateTime.now());
    return e;
  }

  // Mocks behavior of saving settings to database
  private void mockSettingsRepositorySave() {
    when(settingsRepository.saveAllAndFlush(anyList()))
        .thenAnswer(invocation -> {
          final List<SettingsEntity> entities = invocation.getArgument(0);
          entities.forEach(e -> {
            e.setCreatedDate(LocalDateTime.now());
            e.setLastModifiedDate(LocalDateTime.now());
          });
          return entities;
        });
  }
}