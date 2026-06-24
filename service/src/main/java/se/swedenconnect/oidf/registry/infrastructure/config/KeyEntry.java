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

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.util.Assert;

import java.nio.charset.Charset;

/**
 * @param name Name of key
 * @param base64EncodedPublicJwk Base 64 encoded jwk
 * @param certificate Pem certificate
 * @author Felix Hellman
 * @author Per Fredrik Plars
 */
public record KeyEntry(String name, String base64EncodedPublicJwk, String certificate) {
  /**
   * @return parsed key
   */
  public JWK getKey() {
    try {
      if (this.certificate != null) {

        final JWK jwk = JWK.parseFromPEMEncodedObjects(this.certificate);
        return switch (jwk.getKeyType().getValue()) {
          case "RSA" -> new RSAKey.Builder(((RSAKey) jwk).toRSAPublicKey())
              .keyUse(jwk.getKeyUse())
              .algorithm(jwk.getAlgorithm())
              .keyIDFromThumbprint()
              .build();
          case "EC" -> new ECKey.Builder(jwk.toECKey())
              .keyUse(jwk.getKeyUse())
              .algorithm(jwk.getAlgorithm())
              .keyIDFromThumbprint()
              .build();
          default -> throw new IllegalArgumentException("Unsupported key type: " + jwk.getKeyType());
        };
      }
      return JWK.parse(new String(Base64.decode(this.base64EncodedPublicJwk), Charset.defaultCharset()));
    }
    catch (final Exception e) {
      throw new RuntimeException("Failed to load additional key", e);
    }
  }

  /**
   * Validate content
   */
  public void validate() {
    final String basePropName = "openid.federation.registry.instance[].oidf_service_api_validation_key";

    Assert.hasText(this.name, basePropName + "name must be set");

    final boolean hasJwk = this.base64EncodedPublicJwk != null && !this.base64EncodedPublicJwk.isEmpty();
    final boolean hasCert = this.certificate != null && !this.certificate.isEmpty();

    Assert.isTrue(hasJwk ^ hasCert,
        String.format("Exactly one of %1$sbase64EncodedPublicJwk or %1$scertificate must be set", basePropName));

    this.getKey();
  }

}

