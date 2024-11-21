/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.config;

import com.nimbusds.jose.jwk.JWK;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.Base64;

/**
 * Properties for RegistryService
 *
 * @author Per Fredrik Plars
 * @param federationserviceapiSignKey Key used to sign outgoing JWT for federationapi.
 */
@ConfigurationProperties("openid.federation.registry")
public record RegistryProperties(String federationserviceapiSignKey) {

  /**
   * Validate properties
   */
  @PostConstruct
  public void validate(){
    Assert.hasText(federationserviceapiSignKey,"Expected federationserviceapiSignKey");
    try {
      federationserviceapiSignKey();
    }
    catch (Exception e) {
      throw new IllegalArgumentException("FederationserviceapiSignKey has the wrong format can not be parsed",e);
    }
  }

  /**
   * Parsing  federationserviceapiSignKey to a JWK
   * @return JWK
   * @throws ParseException If there is an error to pars signkey
   */
  public JWK federationserviceapiSignKeyJWK() throws ParseException {
    return JWK.parse(new String(Base64.getDecoder().decode(federationserviceapiSignKey), Charset.defaultCharset()));
  }
}
