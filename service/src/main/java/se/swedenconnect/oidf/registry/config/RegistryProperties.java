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

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Properties for RegistryService
 *
 * @param federationServiceApi federation API settings.
 * @param instances InstanceProperties that is managed by this registry
 * @author Per Fredrik Plars
 */
@ConfigurationProperties("openid.federation.registry")
public record RegistryProperties(FederationAPIProperties federationServiceApi, List<InstanceProperties> instances) {

  /**
   * Validates the registry properties to ensure all required fields are properly configured.
   * Checks that federationServiceApi and instances are not null, instances list is not empty,
   * and that at most one instance is marked for default assignment.
   */
  @PostConstruct
  public void validate() {
    Assert.notNull(this.federationServiceApi,
        "openid.federation.registry.federationServiceApi Is needed in configuration");
    this.federationServiceApi.validate();

    Assert.notNull(this.instances, "openid.federation.registry.instances "
        + "properties has to be defined");

    Assert.isTrue(!this.instances.isEmpty(), "openid.federation.registry.instances "
        + "properties has to be defined with at least one instance entry");

    this.instances.forEach(InstanceProperties::validate);

    Assert.isTrue(this.instances.stream().filter(InstanceProperties::useForDefaultAssignment).count() <= 1,
        "openid.federation.registry.instances[].useForDefaultAssignment shall only be set for one instance");
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
   * @param kidAlgorithm serial will create a kid with the serial
   */
  public record FederationAPIProperties(String signKeyAlias,
      String issuer,
      Duration tokenExpiryDuration,
      List<NotificationProperties> notifications,
      boolean notificationActive,
      String notificationTrustKeyAlias,
      String kidAlgorithm
  ) {

    /**
     * Validates the federation API properties to ensure all required fields are properly configured.
     * Checks that signKeyAlias, tokenExpiryDuration, and issuer are set. If notifications are active,
     * validates that notification endpoints are configured.
     */
    public void validate() {
      Assert.hasText(
          this.signKeyAlias, "Expected openid.federation.registry.federationServiceApi.sign_key_alias");
      Assert.notNull(
          this.tokenExpiryDuration,
          "Expected openid.federation.registry.federationServiceApi.token-expiry-duration");
      Assert.hasText(this.issuer, "Expected openid.federation.registry.federationServiceApi.issuer");

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
            this.endpoint, "Expected openid.federation.registry.federationServiceApi.notifications[].endpoint");
        Assert.notNull(
            this.instanceId,
            "Expected openid.federation.registry.federationServiceApi.notifications[].instance_id");
      }
    }
  }

  /**
   * Represents properties for the instance within the registry. This record holds details such as an instance's unique
   * identifier, name, a flag indicating if it should be used for default assignment, and a list of organizational
   * numbers.
   *
   * @param instanceId the unique identifier for the instance must not be null
   * @param name the name of the instance must not be empty
   * @param useForDefaultAssignment flag indicating if this instance should be used for the default assignment
   * @param org_numbers a list of organizational numbers associated with the instance
   */
  public record InstanceProperties(UUID instanceId,
      String name,
      boolean useForDefaultAssignment,
      List<String> org_numbers) {
    /**
     * Validates the instance properties to ensure all required fields are properly configured.
     * Checks that instanceId and name are set. If org_numbers is empty, useForDefaultAssignment must be true.
     */
    public void validate() {
      Assert.notNull(
          this.instanceId, "Expected openid.federation.registry.instances[].instance_id");
      Assert.hasText(this.name, "Expected openid.federation.registry.instances[].name");

      if (this.org_numbers == null || this.org_numbers.isEmpty()) {
        Assert.isTrue(this.useForDefaultAssignment, "If openid.federation.registry.instances[].org_numbers is empty, "
            + "useForDefaultAssignment must be true");
      }

    }
  }
}
