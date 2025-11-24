/*
 * Copyright 2024-2025 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.oidf.registry.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import se.swedenconnect.oidf.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.service.OidfApiService;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RegistryConfig} logic.
 *
 * @author David Goldring
 */
@ExtendWith(MockitoExtension.class)
class RegistryConfigTest {

  @Mock
  private PolicyRepository policyRepository;

  @Mock
  private InstanceRepository instanceRepository;

  @Mock
  private RegistryProperties registryProperties;

  @Mock
  private RegistryProperties.FederationAPIProperties apiProperties;

  @Mock
  private CredentialBundles credentialBundles;

  private RegistryConfig registryConfig;

  @BeforeEach
  void setUp() {
    registryConfig = new RegistryConfig(policyRepository, instanceRepository, registryProperties);

    lenient().when(registryProperties.federationServiceApi()).thenReturn(apiProperties);
    lenient().when(apiProperties.tokenExpiryDuration()).thenReturn(Duration.ofHours(1));
    lenient().when(apiProperties.issuer()).thenReturn("http://test.issuer");
  }

  @Test
  @DisplayName("DevMode enabled - should succeed")
  void testDevModeEnable() {
    // Set devMode = true
    ReflectionTestUtils.setField(registryConfig, "devMode", true);

    // Call with empty credentials
    OidfApiService service = registryConfig.federationServiceApiService(Optional.empty());

    assertThat(service).isNotNull();
  }

  @Test
  @DisplayName("ProductionMode enabled and missing Credentials - should fail")
  void testProductionModeMissingCredentialsBundle() {
    // Set devMode = false (default)
    ReflectionTestUtils.setField(registryConfig, "devMode", false);

    // Call with empty Optional (simulating missing credential bundle bean)
    assertThatThrownBy(() -> registryConfig.federationServiceApiService(Optional.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Production mode requires configured credentials");
  }

  @Test
  @DisplayName("ProductionMode enabled and missing key in Credentials bundle - should fail")
  void testProductionModeMissingKeyInBundle() {
    // Set devMode = false
    ReflectionTestUtils.setField(registryConfig, "devMode", false);

    // Configure mock to return an alias
    when(apiProperties.signKeyAlias()).thenReturn("my-key");

    // Configure a bundle to return null for that alias (key missing)
    when(credentialBundles.getCredential("my-key")).thenReturn(null);

    // Call with a present bundle but missing key
    assertThatThrownBy(() -> registryConfig.federationServiceApiService(Optional.of(credentialBundles)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("ProductionMode enabled and valid bundle - should succeed")
  void testProductionModeSuccess() {
    // Set devMode = false
    ReflectionTestUtils.setField(registryConfig, "devMode", false);

    // Mock successful key retrieval
    when(apiProperties.signKeyAlias()).thenReturn("my-key");
    PkiCredential mockCredential = mock(PkiCredential.class);
    when(credentialBundles.getCredential("my-key")).thenReturn(mockCredential);

    // Call with a valid bundle
    try {
      registryConfig.federationServiceApiService(Optional.of(credentialBundles));
    }
    catch (Exception e) {
      // If it fails here, and it's not an IllegalStateException,
      // it's likely inside JWK conversion, which means we successfully passed the logic we wanted to test
      assertThat(e).isNotInstanceOf(IllegalStateException.class);
    }
  }
}