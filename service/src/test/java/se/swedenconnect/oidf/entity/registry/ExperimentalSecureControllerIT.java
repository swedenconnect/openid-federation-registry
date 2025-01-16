package se.swedenconnect.oidf.entity.registry;

import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

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
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class ExperimentalSecureControllerIT {
  @Autowired
  TestRestTemplate testRestTemplate;

  @Test
  void handleGetRequest() {

    testRestTemplate.getRestTemplate().getInterceptors()
        .add((request, body, execution) -> {
          request.getHeaders().add("Authorization", "Bearer " + createJwt());
          return execution.execute(request, body);
        });
    String url = "/secure";
    final ResponseEntity<String> response = testRestTemplate.getForEntity(url, String.class);
    assertNotNull(response);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void handlePostRequest() {
    testRestTemplate.getRestTemplate().getInterceptors()
        .add((request, body, execution) -> {
          request.getHeaders().add("Authorization", "Bearer " + createJwt());
          return execution.execute(request, body);
        });
    final String url = "/secure";
    final String requestBody = "sample data";
    final ResponseEntity<String> response = testRestTemplate.postForEntity(url, requestBody, String.class);
    assertNotNull(response);
    assertEquals(HttpStatusCode.valueOf(403), response.getStatusCode());
  }

  @Test
  void handlePostPoliciesRequest() {
    testRestTemplate.getRestTemplate().getInterceptors()
        .add((request, body, execution) -> {
          request.getHeaders().add("Authorization", "Bearer " + createJwt());
          return execution.execute(request, body);
        });
    String url = "/secure/policies";
    String requestBody = "sample data";
    final ResponseEntity<String> response = testRestTemplate.postForEntity(url, requestBody, String.class);
    assertNotNull(response);
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
  }

  private String createJwt() {
    try {
      final JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
          .subject("test-user")
          .audience("https://registry.local.swedenconnect.se")
          .issueTime(new java.util.Date())
          .expirationTime(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
          .issuer("http://swedenconnect.se/op")
          .claim("scope", "entity_read entity_write policies_read policies_write "
              + "trustmarksubject_read trustmarksubject_write")
          .build();

      final RSASSASigner signer = new RSASSASigner(getPrivateKeyFromKeyStore());
      final SignedJWT signedJwt = new com.nimbusds.jwt.SignedJWT(
          new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.RS256),
          claims
      );
      signedJwt.sign(signer);
      return signedJwt.serialize();
    } catch (Exception e) {
      throw new RuntimeException("Error creating JWT", e);
    }
  }

  private PrivateKey getPrivateKeyFromKeyStore()
      throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
    final InputStream keyStoreStream = this.getClass().getResourceAsStream("/keystore.jks");
    final KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(keyStoreStream, "changeit".toCharArray());
    final Key key = keyStore.getKey("auth", "changeit".toCharArray());
    return (java.security.PrivateKey) key;
  }
}