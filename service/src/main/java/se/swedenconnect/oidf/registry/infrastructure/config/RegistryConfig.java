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
package se.swedenconnect.oidf.registry.infrastructure.config;

import com.nimbusds.jose.jwk.JWK;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultRoutePlanner;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.registry.entity.repository.EntityRepository;
import se.swedenconnect.oidf.registry.federationservice.service.NotifyService;
import se.swedenconnect.oidf.registry.federationservice.service.OidfApiService;
import se.swedenconnect.oidf.registry.organization.model.Instance;
import se.swedenconnect.oidf.registry.organization.repository.InstanceRepository;
import se.swedenconnect.oidf.registry.subordinate.repository.SubordinateRepository;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import javax.net.ssl.SSLContext;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Spring configuration class that defines beans for different implementations of the EntityService interface.
 *
 * @author Per Fredrik Plars
 * @author David Goldring
 */
@Slf4j
@Configuration
public class RegistryConfig {

  private final InstanceRepository instanceRepository;
  private final SubordinateRepository subordinateRepository;
  private final EntityRepository entityRepository;

  private final RegistryProperties registryProperties;

  /**
   * Constructs a RegistryConfig object, initializing various repositories and services required for the registry
   * configuration.
   *
   * @param subordinateRepository a repository for managing policies
   * @param instanceRepository a repository for managing instance data
   * @param registryProperties the configuration properties for the registry
   * @param entityRepository the configuration properties for the entityRepository
   */
  public RegistryConfig(final SubordinateRepository subordinateRepository,
      final InstanceRepository instanceRepository,
      final RegistryProperties registryProperties,
      final EntityRepository entityRepository) {
    this.subordinateRepository = subordinateRepository;
    this.instanceRepository = instanceRepository;
    this.registryProperties = registryProperties;
    this.entityRepository = entityRepository;
  }

  /**
   * Provides an instance of the FederationApiService for managing federation-related operations.
   *
   * @param credentialBundles Bundle for reading keys
   * @return an instance of FederationApiService configured with the necessary dependencies.
   */
  @Bean
  public OidfApiService federationServiceApiService(
      final Optional<CredentialBundles> credentialBundles) {

    final RegistryProperties.FederationAPIProperties federationAPIProperties =
        this.registryProperties.federationServiceApi();

    final JWK jwk = this.resolveSigningKey(credentialBundles, federationAPIProperties);
    final Duration tokenExpiryDuration = federationAPIProperties.tokenExpiryDuration();

    return new OidfApiService(
        jwk,
        this.subordinateRepository,
        this.entityRepository,
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
      final Optional<CredentialBundles> credentialBundles) {

    final RegistryProperties.FederationAPIProperties federationAPIProperties =
        this.registryProperties.federationServiceApi();

    final JWK jwk = this.resolveSigningKey(credentialBundles, federationAPIProperties); //function.apply(signKey);

    return new NotifyService(restClient,
        federationAPIProperties.notifications(),
        jwk
    );
  }

  /**
   * Initializes instances by creating a database {@link Instance} row for every instance in the registry
   * configuration that doesn't already have one. Matching between configuration and database is always done on
   * {@code instanceId}. Instances found in the database that are not present in the current configuration are not
   * touched, only logged as a warning, since removing an instance's data is not something this should do
   * automatically.
   */
  @EventListener(ContextRefreshedEvent.class)
  void initInstance() {
    log.info("Initializing instances from registry properties");

    this.registryProperties.instances().forEach(instance -> {
      if (this.instanceRepository.existsById(instance.instanceId())) {
        return;
      }
      log.info("Creating new instance in database for instanceId:{} name:{}",
          instance.instanceId(), instance.name());
      final Instance newEntity = new Instance();
      newEntity.setInstanceId(instance.instanceId());
      newEntity.setName(instance.name());
      // No authenticated principal exists at this point for the @CreatedBy/@LastModifiedBy auditing
      // listener to pick up, so set them explicitly.
      newEntity.setCreatedBy("Registry-Config");
      newEntity.setLastModifiedBy(newEntity.getCreatedBy());
      this.instanceRepository.saveAndFlush(newEntity);
    });

    final Set<UUID> configuredInstanceIds = this.registryProperties.instances().stream()
        .map(RegistryProperties.InstanceProperties::instanceId)
        .collect(Collectors.toSet());

    this.instanceRepository.findAll().stream()
        .map(Instance::getInstanceId)
        .filter(instanceId -> !configuredInstanceIds.contains(instanceId))
        .forEach(instanceId -> log.warn(
            "Instance with instanceId:{} exists in the database but is not matched by any instance in the "
                + "current configuration", instanceId));
  }

  /**
   * REST client configured with SSL trust context and observation registry.
   *
   * @param ssl the SSL bundles for trust configuration
   * @param observationRegistry the observation registry for metrics
   * @return the configured rest client
   */
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

  @Bean
  RestClient jwksLoaderRestClient(final RegistryProperties properties,
      final SslBundles ssl,
      final ObservationRegistry observationRegistry) throws NoSuchAlgorithmException, KeyManagementException {

    final HttpClientBuilder httpClientBuilder = HttpClients.custom()
        .disableRedirectHandling()
        .setDefaultRequestConfig(RequestConfig.custom()
            .setResponseTimeout(Timeout.ofSeconds(5))
            .build())

        .setRoutePlanner((host, context) -> {

          if (host == null) {
            throw new SecurityException("Host is missing");
          }

          try {
            final InetAddress addr = InetAddress.getByName(host.getHostName());

            final RegistryProperties.EntityConfigurationLoaderProperties jwksLoaderConf =
                properties.entityConfigurationLoader();

            if (jwksLoaderConf == null || !properties.entityConfigurationLoader().isEnabled()) {
              throw new SecurityException("JWKS loader is not enabled");
            }

            if (!"https".equalsIgnoreCase(host.getSchemeName())) {
              throw new SecurityException("Only HTTPS is allowed, got: " + host.getSchemeName());
            }

            if (!jwksLoaderConf.isEnableLocalIpAddressRanges() && (
                addr.isAnyLocalAddress() ||
                    addr.isLoopbackAddress() ||
                    addr.isSiteLocalAddress())) {
              throw new SecurityException("Blocked internal address");
            }

            if (addr instanceof Inet6Address ipv6Addr) {
              final byte[] ipv6 = ipv6Addr.getAddress();
              if (!jwksLoaderConf.isEnableLocalIpAddressRanges() && (ipv6[0] & 0xFE) == 0xFC) {
                throw new SecurityException("Blocked internal address");
              }
            }

            Optional.ofNullable(jwksLoaderConf.getBlockHostname())
                .ifPresent(block -> block.forEach(regex -> {
                  if (Pattern.compile(regex).matcher(host.getHostName()).find()) {
                    throw new SecurityException("Match in block list: " + host.getHostName() + "->" + regex);
                  }
                  else {
                    log.debug("No block match for hostname: {} {}", host.getHostName(), regex);
                  }
                }));
            return new DefaultRoutePlanner(null).determineRoute(host, context);
          }
          catch (final UnknownHostException e) {
            throw new SecurityException("UnknownHostException", e);
          }

        });

    Optional.ofNullable(properties.entityConfigurationLoader())
        .filter(RegistryProperties.EntityConfigurationLoaderProperties::isDisableSystemProperties)
        .ifPresent(aBoolean -> httpClientBuilder.useSystemProperties());

    final PoolingHttpClientConnectionManagerBuilder cm =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(5)
            .setMaxConnPerRoute(1)
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(2))
                .build());

    final String trustBundleAlias = Optional.ofNullable(properties.entityConfigurationLoader())
        .map(RegistryProperties.EntityConfigurationLoaderProperties::getTrustBundleAlias)
        .orElse(null);

    this.getSSLTrustContext(ssl, trustBundleAlias).ifPresentOrElse(sslContext -> {
      final TlsSocketStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
          .setSslContext(sslContext)
          .buildClassic();
      cm.setTlsSocketStrategy(tlsStrategy);
    }, () -> {
      cm.setTlsSocketStrategy(ClientTlsStrategyBuilder.create().buildClassic());
    });

    httpClientBuilder.setConnectionManager(cm.build());
    return RestClient.builder()
        .observationRegistry(observationRegistry)
        .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build()))
        .build();
  }

  private Optional<SSLContext> getSSLTrustContext(
      final SslBundles ssl,
      final String trustBundleAlias) throws NoSuchAlgorithmException, KeyManagementException {
    if (trustBundleAlias != null && !trustBundleAlias.isBlank()) {
      final SSLContext sslContext = SSLContext.getInstance("TLS");
      final SslBundle bundle = ssl.getBundle(trustBundleAlias);
      Assert.notNull(bundle, "No spring.ssl.bundle found for alias:'%s'".formatted(trustBundleAlias));
      sslContext.init(null, bundle.getManagers().getTrustManagerFactory().getTrustManagers(),
          new java.security.SecureRandom());
      return Optional.of(sslContext);

    }
    else {
      log.info("No trust bundle alias found, using plain http connector");
    }
    return Optional.empty();
  }

  /**
   * Resolves the signing key to be used based on the provided credential bundles and federation API properties.
   *
   * @param credentialBundles the credential bundle containing the necessary keys and certificates.
   * @param federationApiProperties the federation API properties that define key-related configurations.
   * @return the resolved signing key as a {@link JWK} instance.
   * @throws IllegalStateException if no signing key is found for the specified alias.
   */
  private JWK resolveSigningKey(final Optional<CredentialBundles> credentialBundles,
      final RegistryProperties.FederationAPIProperties federationApiProperties) {
    final CredentialBundles bundles = credentialBundles.orElseThrow(() ->
        new IllegalStateException("Missing required 'credential.bundles' configuration."));

    final String signKeyAlias = federationApiProperties.signKeyAlias();
    final PkiCredential sighKey = bundles.getCredential(signKeyAlias);

    final JwkTransformerFunction function = new JwkTransformerFunction();
    if ("serial".equalsIgnoreCase(federationApiProperties.kidAlgorithm())) {
      function.withKeyIdFunction(pkiCredential ->
          Objects.requireNonNull(pkiCredential.getCertificate())
              .getSerialNumber().toString(10));
    }

    return function.apply(sighKey);
  }
}
