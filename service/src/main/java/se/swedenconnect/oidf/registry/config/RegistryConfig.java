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
package se.swedenconnect.oidf.registry.config;

import com.nimbusds.jose.jwk.JWK;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;
import se.swedenconnect.oidf.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.registry.service.NotifyService;
import se.swedenconnect.oidf.registry.service.OidfApiService;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A Spring configuration class that defines beans for different implementations of the EntityService interface.
 *
 * @author Per Fredrik Plars
 * @author David Goldring
 */
@Slf4j
@Configuration
public class RegistryConfig {

  final InstanceRepository instanceRepository;
  private final PolicyRepository policyRepository;

  private final RegistryProperties registryProperties;

  /**
   * Constructs a RegistryConfig object, initializing various repositories and services required for the registry
   * configuration.
   *
   * @param policyRepository a repository for managing policies
   * @param instanceRepository a repository for managing instance data
   * @param registryProperties the configuration properties for the registry
   */
  public RegistryConfig(final PolicyRepository policyRepository,
      final InstanceRepository instanceRepository,
      final RegistryProperties registryProperties) {
    this.policyRepository = policyRepository;

    this.instanceRepository = instanceRepository;
    this.registryProperties = registryProperties;
  }

  private static boolean hasOrg(final String org_nr, final InstanceEntity entity) {
    if (entity.getOrganizations() == null) {
      return false;
    }
    return entity.getOrganizations()
        .stream()
        .noneMatch(o -> o.getOrgNumber().equals(org_nr));
  }

  /**
   * Provides an instance of the FederationApiService for managing federation-related operations.
   *
   * @param credentialBundles Bundle for reading keys
   * @return an instance of FederationApiService configured with the necessary dependencies.
   */
  @Bean
  public OidfApiService federationServiceApiService(
      final CredentialBundles credentialBundles) {

    final RegistryProperties.FederationAPIProperties federationAPIProperties =
        this.registryProperties.federationServiceApi();

    final String signKeyAlias = federationAPIProperties.signKeyAlias();
    final Duration tokenExpiryDuration = federationAPIProperties.tokenExpiryDuration();

    final PkiCredential signKey = credentialBundles.getCredential(signKeyAlias);
    final JwkTransformerFunction function = new JwkTransformerFunction();
    if ("serial".equalsIgnoreCase(federationAPIProperties.kidAlgorithm())) {
      function.setKeyIdFunction(pkiCredential ->
          Objects.requireNonNull(pkiCredential.getCertificate())
              .getSerialNumber().toString(10));
    }

    final JWK jwk = function.apply(signKey);

    return new OidfApiService(
        jwk,
        this.policyRepository,
        federationAPIProperties.issuer(),
        this.instanceRepository,
        tokenExpiryDuration
    );
  }

  /**
   * Creates and configures a {@link NotifyService} instance to handle notifications for the Federation API. This method
   * sets up the notification endpoints and the signing key to secure the communication.
   *
   * @param restClient the {@link RestClient} instance used to perform HTTP requests.
   * @param credentialBundles the {@link CredentialBundles} providing access to credentials used when signing
   *     notifications.
   * @return a configured {@link NotifyService} instance.
   */
  @Bean
  @ConditionalOnProperty(prefix = "openid.federation.registry.federation_service_api", name = "notification_active",
      havingValue = "true")
  public NotifyService notificationService(
      final RestClient restClient,
      final CredentialBundles credentialBundles) {

    final RegistryProperties.FederationAPIProperties federationAPIProperties =
        this.registryProperties.federationServiceApi();

    final String signKeyAlias = federationAPIProperties.signKeyAlias();

    final PkiCredential signKey = credentialBundles.getCredential(signKeyAlias);
    final JwkTransformerFunction function = new JwkTransformerFunction();
    final JWK jwk = function.apply(signKey);

    return new NotifyService(restClient,
        federationAPIProperties.notifications().stream().map(
            RegistryProperties.FederationAPIProperties.NotificationProperties::endpoint).toList(),
        jwk
    );
  }

  /**
   * Initializes instances by converting the provided registry properties into entity objects and persisting them to the
   * database.
   */
  @EventListener(ContextRefreshedEvent.class)
  void initInstance() {
    log.info("Initializing instances from registry properties");
    this.registryProperties.instances()
        .forEach(instance -> {

          final InstanceEntity entity = this.instanceRepository.findById(instance.instanceId())
              .or(() -> {
                log.debug("Creating new instance entity for instanceId:{}", instance.instanceId());
                final InstanceEntity newEntity = new InstanceEntity();
                newEntity.setInstanceId(instance.instanceId());
                newEntity.setName(instance.name());
                newEntity.setCreatedBy("Registry-Config");
                newEntity.setLastModifiedBy(newEntity.getCreatedBy());
                newEntity.setUseForDefaultAssignment(instance.useForDefaultAssignment());
                return Optional.of(newEntity);
              }).orElseThrow();

          if (instance.org_numbers() != null) {
            instance.org_numbers()
                .stream()
                .filter(org_nr -> hasOrg(org_nr, entity))
                .map(org_nr -> {
                  log.debug("Adding organization with org_nr:{} to instanceId:{}", org_nr, instance.instanceId());
                  final OrganizationEntity organizationEntity = new OrganizationEntity();
                  organizationEntity.setOrganizationId(UUID.randomUUID());
                  organizationEntity.setOrgNumber(org_nr);
                  organizationEntity.setInstance(entity);
                  organizationEntity.setCreatedBy(entity.getCreatedBy());
                  organizationEntity.setLastModifiedBy(entity.getLastModifiedBy());
                  return organizationEntity;
                })
                .forEach(entity::addOrganization);
          }
          this.instanceRepository.saveAndFlush(entity);
        });
  }

  @Primary
  @Bean
  RestClient restClient(final SslBundles ssl,
      final ObservationRegistry observationRegistry) throws Exception {

    final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
    final String trustBundleAlias = this.registryProperties.federationServiceApi().notificationTrustKeyAlias();
    this.settingSSLTrustContext(ssl, httpClientBuilder, trustBundleAlias);
    return RestClient.builder()
        .observationRegistry(observationRegistry)
        .requestFactory(new JdkClientHttpRequestFactory(httpClientBuilder.build()))
        .build();
  }

  private void settingSSLTrustContext(final SslBundles ssl, final HttpClient.Builder httpClientBuilder,
      final String trustBundleAlias) throws NoSuchAlgorithmException, KeyManagementException {
    if (trustBundleAlias != null && !trustBundleAlias.isBlank()) {
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      final SslBundle bundle = ssl.getBundle(trustBundleAlias);
      Assert.notNull(bundle, "No spring.ssl.bundle found for alias:'%s'".formatted(trustBundleAlias));
      sslContext.init(null, bundle.getManagers().getTrustManagerFactory().getTrustManagers(),
          new java.security.SecureRandom());
      httpClientBuilder.sslContext(sslContext);
    }
    else {
      log.info("No trust bundle alias found, using plain http connector");
    }
  }

}
