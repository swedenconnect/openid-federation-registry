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
package se.swedenconnect.oidf.registry.guioperations;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.swedenconnect.oidf.registry.guioperations.dto.JwksPayloadDto;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.service.InstancePlacementService;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwksKeysCacheServiceTest {

  @Mock
  OidfServiceIntegration oidfServiceIntegration;

  @Mock
  InstancePlacementService instancePlacementService;

  JwksKeysCacheService service;

  final URI instanceUrl = URI.create("https://registry.example.se/oidf");
  final OrganizationRecord org = new OrganizationRecord("55555", "PM", "https://www.pm.se/oidf/", null);

  JWK validationKey;

  @BeforeEach
  void setUp() throws Exception {
    service = new JwksKeysCacheService(oidfServiceIntegration, instancePlacementService);
    validationKey = rsaKey("validation-key");
  }

  // -------------------------------------------------------------------------
  // getFederationKeys
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Returns federation keys for the org's instance")
  void getFederationKeys_returnsKeysFromCorrectInstance() throws Exception {
    final JWKSet fedKeys = new JWKSet(rsaKey("fed-kid-1"));
    final JwksPayloadDto payload = new JwksPayloadDto(fedKeys, new JWKSet(), JwksPayloadDto.KeyNames.empty());

    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.of(instanceUrl));
    when(instancePlacementService.resolveValidationKey(org.orgNumber(), org.functionGroup()))
        .thenReturn(Optional.of(validationKey));
    when(oidfServiceIntegration.fetchServiceKeys(EntityID.parse(instanceUrl.toString()), validationKey))
        .thenReturn(payload);

    final List<JWK> result = service.getFederationKeys(org);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKeyID()).isEqualTo("fed-kid-1");
  }

  @Test
  @DisplayName("Returns hosted keys for the org's instance")
  void getHostedKeys_returnsKeysFromCorrectInstance() throws Exception {
    final JWKSet hostedKeys = new JWKSet(rsaKey("hosted-kid-1"));
    final JwksPayloadDto payload = new JwksPayloadDto(new JWKSet(), hostedKeys, JwksPayloadDto.KeyNames.empty());

    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.of(instanceUrl));
    when(instancePlacementService.resolveValidationKey(org.orgNumber(), org.functionGroup()))
        .thenReturn(Optional.of(validationKey));
    when(oidfServiceIntegration.fetchServiceKeys(EntityID.parse(instanceUrl.toString()), validationKey))
        .thenReturn(payload);

    final List<JWK> result = service.getHostedKeys(org);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKeyID()).isEqualTo("hosted-kid-1");
  }

  @Test
  @DisplayName("Returns empty list when no instance found for org")
  void returnsEmptyWhenNoInstanceFound() {
    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.empty());

    assertThat(service.getFederationKeys(org)).isEmpty();
    assertThat(service.getHostedKeys(org)).isEmpty();
    verifyNoInteractions(oidfServiceIntegration);
  }

  @Test
  @DisplayName("Throws when instance has no oidf_service_api_validation_key configured")
  void throwsWhenValidationKeyMissing() {
    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.of(instanceUrl));
    when(instancePlacementService.resolveValidationKey(org.orgNumber(), org.functionGroup()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getFederationKeys(org))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("oidf_service_api_validation_key")
        .hasMessageContaining(org.orgNumber());
  }

  // -------------------------------------------------------------------------
  // Cache fallback behaviour
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Returns cached result when fetch fails after initial success")
  void returnsCachedResultOnFetchFailure() throws Exception {
    final JWKSet fedKeys = new JWKSet(rsaKey("fed-kid-1"));
    final JwksPayloadDto cached = new JwksPayloadDto(fedKeys, new JWKSet(), JwksPayloadDto.KeyNames.empty());

    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.of(instanceUrl));
    when(instancePlacementService.resolveValidationKey(org.orgNumber(), org.functionGroup()))
        .thenReturn(Optional.of(validationKey));
    when(oidfServiceIntegration.fetchServiceKeys(any(), any()))
        .thenReturn(cached)
        .thenThrow(new RuntimeException("network error"));

    service.getFederationKeys(org);                 // warms cache
    final List<JWK> result = service.getFederationKeys(org); // should use cache

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getKeyID()).isEqualTo("fed-kid-1");
    verify(oidfServiceIntegration, times(2)).fetchServiceKeys(any(), any());
  }

  @Test
  @DisplayName("Returns empty list when fetch fails and cache is cold")
  void returnsEmptyWhenFetchFailsAndCacheIsCold() throws Exception {
    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.of(instanceUrl));
    when(instancePlacementService.resolveValidationKey(org.orgNumber(), org.functionGroup()))
        .thenReturn(Optional.of(validationKey));
    when(oidfServiceIntegration.fetchServiceKeys(any(), any()))
        .thenThrow(new RuntimeException("network error"));

    assertThat(service.getFederationKeys(org)).isEmpty();
  }

  @Test
  @DisplayName("Cache is refreshed on every successful call")
  void cacheIsAlwaysRefreshedOnSuccess() throws Exception {
    final JWKSet keysV1 = new JWKSet(rsaKey("kid-v1"));
    final JWKSet keysV2 = new JWKSet(rsaKey("kid-v2"));

    when(instancePlacementService.resolveBaseUrl(org)).thenReturn(Optional.of(instanceUrl));
    when(instancePlacementService.resolveValidationKey(org.orgNumber(), org.functionGroup()))
        .thenReturn(Optional.of(validationKey));
    when(oidfServiceIntegration.fetchServiceKeys(any(), any()))
        .thenReturn(new JwksPayloadDto(keysV1, new JWKSet(), JwksPayloadDto.KeyNames.empty()))
        .thenReturn(new JwksPayloadDto(keysV2, new JWKSet(), JwksPayloadDto.KeyNames.empty()));

    service.getFederationKeys(org);
    final List<JWK> result = service.getFederationKeys(org);

    assertThat(result.get(0).getKeyID()).isEqualTo("kid-v2");
  }

  // -------------------------------------------------------------------------
  // Multi-instance isolation
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("Two orgs on different instances receive keys from their respective instance")
  void differentOrgsGetKeysFromCorrectInstance() throws Exception {
    final URI urlA = URI.create("https://instance-a.example.se/oidf");
    final URI urlB = URI.create("https://instance-b.example.se/oidf");
    final OrganizationRecord orgA = new OrganizationRecord("11111", "OrgA", "https://a.example.se/", null);
    final OrganizationRecord orgB = new OrganizationRecord("22222", "OrgB", "https://b.example.se/", null);
    final JWK keyA = rsaKey("validation-key-a");
    final JWK keyB = rsaKey("validation-key-b");

    final JWKSet keysA = new JWKSet(rsaKey("kid-instance-a"));
    final JWKSet keysB = new JWKSet(rsaKey("kid-instance-b"));

    when(instancePlacementService.resolveBaseUrl(orgA)).thenReturn(Optional.of(urlA));
    when(instancePlacementService.resolveBaseUrl(orgB)).thenReturn(Optional.of(urlB));
    when(instancePlacementService.resolveValidationKey(orgA.orgNumber(), orgA.functionGroup()))
        .thenReturn(Optional.of(keyA));
    when(instancePlacementService.resolveValidationKey(orgB.orgNumber(), orgB.functionGroup()))
        .thenReturn(Optional.of(keyB));
    when(oidfServiceIntegration.fetchServiceKeys(EntityID.parse(urlA.toString()), keyA))
        .thenReturn(new JwksPayloadDto(keysA, new JWKSet(), JwksPayloadDto.KeyNames.empty()));
    when(oidfServiceIntegration.fetchServiceKeys(EntityID.parse(urlB.toString()), keyB))
        .thenReturn(new JwksPayloadDto(new JWKSet(), keysB, JwksPayloadDto.KeyNames.empty()));

    assertThat(service.getFederationKeys(orgA).get(0).getKeyID()).isEqualTo("kid-instance-a");
    assertThat(service.getHostedKeys(orgB).get(0).getKeyID()).isEqualTo("kid-instance-b");
  }

  @Test
  @DisplayName("Failure for one instance does not affect cache of another instance")
  void failureForOneInstanceDoesNotAffectOther() throws Exception {
    final URI urlA = URI.create("https://instance-a.example.se/oidf");
    final URI urlB = URI.create("https://instance-b.example.se/oidf");
    final OrganizationRecord orgA = new OrganizationRecord("11111", "OrgA", "https://a.example.se/", null);
    final OrganizationRecord orgB = new OrganizationRecord("22222", "OrgB", "https://b.example.se/", null);
    final JWK keyA = rsaKey("validation-key-a");
    final JWK keyB = rsaKey("validation-key-b");

    final JWKSet keysA = new JWKSet(rsaKey("kid-a"));

    when(instancePlacementService.resolveBaseUrl(orgA)).thenReturn(Optional.of(urlA));
    when(instancePlacementService.resolveBaseUrl(orgB)).thenReturn(Optional.of(urlB));
    when(instancePlacementService.resolveValidationKey(orgA.orgNumber(), orgA.functionGroup()))
        .thenReturn(Optional.of(keyA));
    when(instancePlacementService.resolveValidationKey(orgB.orgNumber(), orgB.functionGroup()))
        .thenReturn(Optional.of(keyB));
    when(oidfServiceIntegration.fetchServiceKeys(EntityID.parse(urlA.toString()), keyA))
        .thenReturn(new JwksPayloadDto(keysA, new JWKSet(), JwksPayloadDto.KeyNames.empty()));
    when(oidfServiceIntegration.fetchServiceKeys(EntityID.parse(urlB.toString()), keyB))
        .thenThrow(new RuntimeException("instance B unreachable"));

    final List<JWK> resultA = service.getFederationKeys(orgA);
    final List<JWK> resultB = service.getFederationKeys(orgB);

    assertThat(resultA).hasSize(1);
    assertThat(resultB).isEmpty();
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  private static JWK rsaKey(final String kid) throws Exception {
    return new RSAKeyGenerator(2048)
        .keyID(kid)
        .keyUse(KeyUse.SIGNATURE)
        .generate()
        .toPublicJWK();
  }
}