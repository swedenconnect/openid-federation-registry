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

package se.swedenconnect.oidf.registry.fixture;

import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.auth.OrganizationRecord;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static se.swedenconnect.oidf.registry.auth.OrganizationRecordClaimSelector.SELECTED_ORG_NUMBER_HEADER_NAME;

/**
 * Utility class for generating and signing JSON Web Tokens (JWTs) for testing purposes.
 *
 * @author David Goldring
 */
@Component
public class JwtTestUtils {

  /**
   * If a token is needed for external testing
   *
   * @param args No args needed
   */
  public static void main(String[] args) {
    JwtTestUtils jwtTestUtils = new JwtTestUtils();
    System.out.println(jwtTestUtils.createJwt(OrganisationType.PM));
  }

  public void setAuthHeaders(OrganisationType organizationType, HttpHeaders headers) {
    headers.set("Authorization", "Bearer " + this.createJwt(organizationType));
    headers.set(SELECTED_ORG_NUMBER_HEADER_NAME, organizationType.orgId);
  }


  /**
   * Generates and signs a JSON Web Token (JWT) with predefined claims and returns it as a serialized string.
   * <p>
   * The JWT is signed using a private key retrieved from the key store. The corresponding public key for checking the
   * signature is set in:
   * <pre>{@code
   *   spring:
   *   security:
   *     oauth2:
   *       resourceserver:
   *         jwt:
   *           public-key-location: classpath:public_auth_cert.pem
   * </pre>
   *
   * This way we don't need to spin up an Authentication Server for the tests.
   *
   * @return A serialized representation of the signed JWT.
   * @throws RuntimeException if an error occurs during the JWT creation or signing process.
   */
  public String createJwt(OrganisationType orgType) {
    try {
      final String scopes = "http://registry.swedenconnect.se/policies/write " +
          "http://registry.swedenconnect.se/policies/read" +
          "http://registry.swedenconnect.se/trustmarksubjects/read " +
          "http://registry.swedenconnect.se/modules/read " +
          "http://registry.swedenconnect.se/modules/write " +
          "http://registry.swedenconnect.se/entity/read " +
          "http://registry.swedenconnect.se/entity/write " +
          "http://registry.swedenconnect.se/trustmarksubjects/write";

      final JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
          .subject("test-user")
          .audience("account")
          .issueTime(new java.util.Date())
          .expirationTime(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
          .issuer("http://swedenconnect.se/op")
          .claim("scope", scopes)

          .claim("org", Arrays.stream(OrganisationType.values())
              .map(o ->
                  Map.of("orgName", o.name,
                      "orgNumber", o.orgId,
                      "entity_prefix", o.domainPrefix))
              .toList())
          .build();

      final RSASSASigner signer = new RSASSASigner(getPrivateKeyFromKeyStore());
      final SignedJWT signedJwt = new com.nimbusds.jwt.SignedJWT(
          new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256),
          claims
      );
      signedJwt.sign(signer);
      return signedJwt.serialize();
    }
    catch (Exception e) {
      throw new RuntimeException("Error creating JWT", e);
    }
  }

  /**
   * We sign our test JWT's with a private key from a snakeoil keystore.
   */
  public PrivateKey getPrivateKeyFromKeyStore() {

    try {
      final InputStream keyStoreStream = this.getClass().getResourceAsStream("/keystore.jks");
      final KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(keyStoreStream, "changeit".toCharArray());
      final Key key = keyStore.getKey("auth", "changeit".toCharArray());

      return (PrivateKey) key;
    }
    catch (final KeyStoreException | IOException |
        NoSuchAlgorithmException | CertificateException |
        UnrecoverableKeyException e) {
      throw new RuntimeException(e);
    }

  }

  public static OrganizationRecord createOrganizationRecord(OrganisationType orgType) {
    return new OrganizationRecord(orgType.orgId, orgType.name, orgType.domainPrefix);
  }

  public enum OrganisationType {
    PM("Pensionsmyndigheten", "55555", "https://www.pm.se/oidf"),
    AF("Arbetsförmedlingen", "66666", "https://www.af.se/oidf"),
    SKATT("Skatteverket", "77777", "https://www.skv.se/oidf"),
    ;

    public final String name;
    public final String orgId;
    public final String domainPrefix;

    OrganisationType(String name, String orgId, String domainPrefix) {
      this.name = name;
      this.orgId = orgId;
      this.domainPrefix = domainPrefix;
    }

  }

}
