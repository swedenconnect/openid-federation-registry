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
package se.swedenconnect.oidf.entity.registry.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Properties for RegistryService
 *
 * @param federationServiceApi federationapi settings.
 * @param instances InstanceProperties that is managed by this registry
 * @author Per Fredrik Plars
 */
@ConfigurationProperties("openid.federation.registry")
public record RegistryProperties(FederationAPIProperties federationServiceApi, List<InstanceProperties> instances) {

  /**
   * Validate properties
   */
  @PostConstruct
  public void validate() {
    Assert.notNull(this.federationServiceApi,
        "openid.federation.registry.federatonServiceApi Is needed in configuration");
    this.federationServiceApi.validate();

    Assert.notNull(this.instances, "openid.federation.registry.instances "
        + "properties has to be defined");

    Assert.isTrue(!this.instances.isEmpty(), "openid.federation.registry.instances "
        + "properties has to be defined with at least one instance entry");

    Optional.of(this.instances)
        .ifPresent(instances -> instances.forEach(InstanceProperties::validate));
  }

  /**
   * Represents the properties for configuring the Federation API in the OpenID Federation Registry Service. This record
   * encapsulates various configuration settings necessary for the Federation API to function correctly, including
   * signing key alias, issuer details, token expiry, notification configurations, and retry mechanisms.
   *
   * @param signKeyAlias Alias for the key used to sign outgoing JWTs.
   * @param issuer The issuer URL to be set in outgoing JWTs.
   * @param tokenExpiryDuration The duration for which a token is valid after issuance.
   * @param notifications The endpoint to which notifications should be sent, if enabled.
   * @param notificationActive Flag indicating whether notifications are enabled.
   * @param notificationTrustKeyAlias Alias to spring trustbundle for outgoing requests
   */
  public record FederationAPIProperties(String signKeyAlias,
      String issuer,
      Duration tokenExpiryDuration,
      List<NotificationProperties> notifications,
      boolean notificationActive,
      String notificationTrustKeyAlias
  ) {

    /**
     * Validate properties
     */
    public void validate() {
      Assert.hasText(
          this.signKeyAlias, "Expected openid.federation.registry.federatonServiceApi.sign_key_alias");
      Assert.notNull(
          this.tokenExpiryDuration,
          "Expected openid.federation.registry.federatonServiceApi.token-expiry-duration");
      Assert.hasText(this.issuer, "Expected openid.federation.registry.federatonServiceApi.issuer");

      if (this.notificationActive) {
        Assert.notNull(this.notifications, "Notification endpoints is required");
        Assert.isTrue(!this.notifications.isEmpty(), "Notification endpoints is required");
        this.notifications.forEach(NotificationProperties::validate);
      }

    }

    /**
     * Represents the properties for notifications in the OpenID Federation Registry Service. This record encapsulates
     * the endpoint and instance identifier necessary for sending notifications as part of the Federation API
     * configuration.
     *
     * @param endpoint The URI of the notification endpoint to which notifications are sent.
     * @param instanceId The unique identifier for the instance sending notifications.
     */
    public record NotificationProperties(URI endpoint, UUID instanceId) {
      /**
       * Validates the properties of a notification instance to ensure all required fields are non-null. This method
       * checks that the `endpoint` and `instanceId` fields are properly initialized. If any of these fields are null,
       * an {@link IllegalArgumentException} will be thrown with a detailed error message indicating the missing
       * property.
       *
       * @throws IllegalArgumentException if either the `endpoint` or `instanceId` property is null.
       */
      public void validate() {
        Assert.notNull(
            this.endpoint, "Expected openid.federation.registry.federatonServiceApi.notifications[].endpoint");
        Assert.notNull(
            this.instanceId,
            "Expected openid.federation.registry.federatonServiceApi.notifications[].instance_id");
      }
    }
  }

  /**
   * Represents the properties of an individual instance managed within the registry.
   *
   * @param instanceId the unique identifier of the instance.
   * @param name the name of the instance.
   */
  public record InstanceProperties(UUID instanceId, String name) {
    /**
     * Validate properties
     */
    public void validate() {
      Assert.notNull(
          this.instanceId, "Expected openid.federation.registry.instances[].instance_id");
      Assert.hasText(this.name, "Expected openid.federation.registry.instances[].name");

    }
  }
}
