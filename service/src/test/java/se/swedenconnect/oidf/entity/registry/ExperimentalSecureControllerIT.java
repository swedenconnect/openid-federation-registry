package se.swedenconnect.oidf.entity.registry;

import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
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
    var response = testRestTemplate.getForEntity(url, String.class);
    assertNotNull(response);
    assertEquals(403, response.getStatusCodeValue());
  }

  @Test
  void handlePostRequest() {
    testRestTemplate.getRestTemplate().getInterceptors()
        .add((request, body, execution) -> {
          request.getHeaders().add("Authorization", "Bearer " + createJwt());
          return execution.execute(request, body);
        });
    String url = "/secure";
    String requestBody = "sample data";
    var response = testRestTemplate.postForEntity(url, requestBody, String.class);
    assertNotNull(response);
    assertEquals(403, response.getStatusCodeValue());
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
    var response = testRestTemplate.postForEntity(url, requestBody, String.class);
    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
  }

  private String createJwt() {
    try {
      final JWTClaimsSet claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
          .subject("test-user")
          .issueTime(new java.util.Date())
          .expirationTime(new java.util.Date(System.currentTimeMillis() + 1000 * 60 * 10))
          .issuer("http://swedenconnect.se/op")
          .claim("scope", "entity_read entity_write policies_read policies_write "
              + "trustmarksubject_read trustmarksubject_write")
          .build();

      final MACSigner signer = new com.nimbusds.jose.crypto.MACSigner("00000000000000000000000000000000");
      final SignedJWT signedJwt = new com.nimbusds.jwt.SignedJWT(
          new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256),
          claims
      );
      signedJwt.sign(signer);
      return signedJwt.serialize();
    } catch (Exception e) {
      throw new RuntimeException("Error creating JWT", e);
    }
  }
}