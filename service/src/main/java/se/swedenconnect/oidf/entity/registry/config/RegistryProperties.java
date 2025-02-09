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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Properties for RegistryService
 *
 * @author Per Fredrik Plars
 * @param federationServiceApi federationapi settings.
 * @param instances InstanceProperties that is managed by this registry
 */
@ConfigurationProperties("openid.federation.registry")
public record RegistryProperties(FederationAPIProperties federationServiceApi, List<InstanceProperties> instances) {

  /**
   * Validate properties
   */
  @PostConstruct
  public void validate(){
    Assert.notNull(this.federationServiceApi,
        "openid.federation.registry.federatonServiceApi Is needed in configuration");
    this.federationServiceApi.validate();
    Optional.ofNullable(this.instances)
        .ifPresent(instances -> instances.forEach(InstanceProperties::validate));
  }

  /**
   * Properties for Federation API
   * @param signKeyAlias Alias for the key used to sign outgoing JWT:S
   * @param issuer Issuer that is set on outgoing JWT:s
   */
  public record FederationAPIProperties(String signKeyAlias,String issuer){

    /**
     * Validate properties
     */
    public void validate(){
      Assert.hasText(
          this.signKeyAlias,"Expected openid.federation.registry.federatonServiceApi.sign_key_alias");
      Assert.hasText(this.issuer,"Expected openid.federation.registry.federatonServiceApi.issuer");

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
