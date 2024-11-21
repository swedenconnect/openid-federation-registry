package se.swedenconnect.oidf.entity.registry.federationserviceapi;

import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.entity.util.EntityFactory;
import se.swedenconnect.oidf.registry.api.model.Entity;

import java.text.ParseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FederationServiceApiControllerIT {
  @Autowired
  private TestRestTemplate restTemplate;

  /**
   * A static instance of the MariaDBContainer using version 11.2 of the MariaDB image.
   * This container is managed by the Spring framework with the use of {@code @Container}
   * and {@code @ServiceConnection} annotations.
   * The instance facilitates database management for integration testing by providing
   * an isolated database environment.
   */
  @Container
  @ServiceConnection
  public static MariaDBContainer<?> database = new MariaDBContainer<>("mariadb:11.2");


  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void trustMarkRecord() {



  }

  @Test
  void policyRecord() {
  }

  @Test
  void entityRecord() throws ParseException {
    final Entity entity = EntityFactory.createDefaultEntity("http://tmi.digg.se","http://sub.digg.se");
    final ResponseEntity<Entity> createResponse = restTemplate.postForEntity("/registry/v1/entities", entity, Entity.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    final ResponseEntity<String> response = restTemplate
        .getForEntity("/api/v1/federationservice/entity_record?iss=http://tmi.digg.se", String.class);
    System.out.println(response);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    final SignedJWT signedJWT = SignedJWT.parse(response.getBody());

    System.out.println(signedJWT.getJWTClaimsSet().toString(true));

  }
}