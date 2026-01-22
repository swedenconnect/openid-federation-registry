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

package se.swedenconnect.oidf.registry.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;
import se.swedenconnect.oidf.registry.dto.HostedEntityDto;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link ValidateDto#validate(HostedEntityDto)}.
 *
 * @author Per Fredrik Plars
 */
@DisplayName("ValidateDto HostedEntityDto validation tests")
class ValidateDtoHostedEntityTest {

  private static final String VALID_ENTITY_ID = "https://example.com/entity";
  private static final String VALID_ENTITY_PREFIX = "https://www.pm.se/oidf";
  private static final String VALID_EC_LOCATION = "https://example.com/.well-known/openid-federation";
  private OrganizationRecord organizationRecord;

  @BeforeEach
  void setUp() {
    this.organizationRecord = new OrganizationRecord("55555", "Test Organization", VALID_ENTITY_PREFIX);
  }

  @Test
  @DisplayName("Should validate successfully with all required fields and valid values")
  void testValidateSuccessWithAllFields() {
    final HostedEntityDto dto = createValidDto();
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertDoesNotThrow(() -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should validate successfully with ecLocationAutomaticResolve true and valid ecLocation")
  void testValidateSuccessWithEcLocationAutomaticResolve() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocationAutomaticResolve(true);
    dto.setEcLocation(VALID_EC_LOCATION);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertDoesNotThrow(() -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should validate successfully when entityIdentifier starts with entity prefix")
  void testValidateSuccessWithEntityPrefix() {
    final HostedEntityDto dto = createValidDto();
    dto.setEntityIdentifier(VALID_ENTITY_PREFIX + "/entity");
    dto.setEcLocationAutomaticResolve(false);
    dto.setEcLocation(null);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertDoesNotThrow(() -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when entityIdentifier is null")
  void testValidateFailureWhenEntityIdentifierIsNull() {
    final HostedEntityDto dto = createValidDto();
    dto.setEntityIdentifier(null);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when entityIdentifier is empty")
  void testValidateFailureWhenEntityIdentifierIsEmpty() {
    final HostedEntityDto dto = createValidDto();
    dto.setEntityIdentifier("");
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when entityIdentifier is not a valid entity ID")
  void testValidateFailureWhenEntityIdentifierIsInvalid() {
    final HostedEntityDto dto = createValidDto();
    dto.setEntityIdentifier("not-a-valid-entity-id");
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when metadata is null")
  void testValidateFailureWhenMetadataIsNull() {
    final HostedEntityDto dto = createValidDto();
    dto.setMetadata(null);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when ecLocation is not a valid URL")
  void testValidateFailureWhenEcLocationIsInvalidUrl() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocation("not-a-valid-url");
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when ecLocation is null and ecLocationAutomaticResolve is false")
  void testValidateFailureWhenEcLocationIsNullAndAutomaticResolveIsFalse() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocation(null);
    dto.setEcLocationAutomaticResolve(false);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    // When ecLocation is null and automatic resolve is false, entityIdentifier must start with entity prefix
    dto.setEntityIdentifier("https://wrong-prefix.com/entity");
    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when ecLocation is blank and ecLocationAutomaticResolve is false")
  void testValidateFailureWhenEcLocationIsBlankAndAutomaticResolveIsFalse() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocation("   ");
    dto.setEcLocationAutomaticResolve(false);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    // When ecLocation is blank and automatic resolve is false, entityIdentifier must start with entity prefix
    dto.setEntityIdentifier("https://wrong-prefix.com/entity");
    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should throw exception when entityIdentifier does not start with entity prefix when required")
  void testValidateFailureWhenEntityIdentifierDoesNotStartWithPrefix() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocationAutomaticResolve(false);
    dto.setEcLocation(null);
    dto.setEntityIdentifier("https://wrong-prefix.com/entity");
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertThrows(PropertyValidationFailException.class, () -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should validate successfully when ecLocationAutomaticResolve is true even if entityIdentifier doesn't start with prefix")
  void testValidateSuccessWhenEcLocationAutomaticResolveIsTrue() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocationAutomaticResolve(true);
    dto.setEcLocation(VALID_EC_LOCATION);
    dto.setEntityIdentifier("https://different-prefix.com/entity");
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertDoesNotThrow(() -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should validate successfully when ecLocation is provided even if entityIdentifier doesn't start with prefix")
  void testValidateSuccessWhenEcLocationIsProvided() {
    final HostedEntityDto dto = createValidDto();
    dto.setEcLocationAutomaticResolve(false);
    dto.setEcLocation(VALID_EC_LOCATION);
    dto.setEntityIdentifier("https://different-prefix.com/entity");
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertDoesNotThrow(() -> validator.validate(dto));
  }

  @Test
  @DisplayName("Should validate successfully with valid JSON metadata")
  void testValidateSuccessWithValidJsonMetadata() {
    final HostedEntityDto dto = createValidDto();
    final Map<String, Object> validMetadata = new HashMap<>();
    validMetadata.put("federation_entity", Map.of("organization_name", "Test Org"));
    dto.setMetadata(validMetadata);
    final ValidateDto validator = ValidateDto.init(this.organizationRecord);

    assertDoesNotThrow(() -> validator.validate(dto));
  }

  /**
   * Creates a valid HostedEntityDto for testing.
   *
   * @return a valid HostedEntityDto instance
   */
  private HostedEntityDto createValidDto() {
    final HostedEntityDto dto = new HostedEntityDto();
    dto.setEntityIdentifier(VALID_ENTITY_ID);
    final Map<String, Object> metadata = new HashMap<>();
    metadata.put("federation_entity", Map.of("organization_name", "Test Organization"));
    dto.setMetadata(metadata);
    dto.setEcLocation(VALID_EC_LOCATION);
    dto.setEcLocationAutomaticResolve(false);
    return dto;
  }
}

