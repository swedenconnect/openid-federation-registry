
/*
 * Copyright 2024 Sweden Connect.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.oidf.entity.registry.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;
import se.swedenconnect.oidf.registry.api.model.EntityRecordHostedRecord;
import se.swedenconnect.oidf.registry.api.model.PolicyRecord;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the EntityController.
 * <p>
 * This class uses Spring Boot Test framework to load the application context,
 * and Testcontainers for managing an external database container.
 *
 * @author David Goldring
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EntityControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  /**
   * A static instance of MariaDBContainer configured to use the MariaDB 11.2 image.
   * This container is managed by the test framework and is used to provide
   * a MariaDB database connection for testing purposes.
   * Annotated with {@code @Container} to indicate that it is a test container managed
   * by Testcontainers, and
   * <a href="https://docs.spring.io/spring-boot/reference/features/dev-services.html">@ServiceConnection</a>
   * to represent a service connection.
   */
  @Container
  @ServiceConnection
  public static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:11.2");

  /**
   * Tests the creation of an {@code Entity} via HTTP POST request.
   * <p>
   * This method performs the following steps:
   * 1. Creates a new {@code Entity} instance.
   * 2. Sets the subject, location, and policy for the {@code Entity}.
   * 3. Sends a POST request to the `/registry/v1/entities` endpoint.
   * 4. Verifies that the response status is HTTP 201 Created.
   * 5. Validates that the response JSON contains the expected subject value.
   *
   * @throws Exception if an error occurs during the request or validation
   */
  @Test
  public void testCreateEntity() throws Exception {
    final PolicyRecord policyRecord = PolicyRecord.builder()
        .name("TestPolicy")
        .policyRecordId(UUID.randomUUID().toString())
        .policy("{\"Test Policy\":\"value\"}")
        .build();
    this.mockMvc.perform(post("/registry/v1/policies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(policyRecord)))
        .andExpect(status().isCreated());

    final EntityRecord record = new EntityRecord();
    final String subject = "https://example.com/subject/1";

    record.setSubject(subject);
    record.setIssuer("http://issuer.swedenconnect.se");
    record.setPolicyRecordId(policyRecord.getPolicyRecordId());
    record.setIssuer("http://iss.example.se");
    record.setEntityRecordId(UUID.randomUUID().toString());

    this.mockMvc.perform(post("/registry/v1/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(record)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.subject").value(subject));
  }

  /**
   * Tests the retrieval of an {@code Entity} via HTTP GET request.
   * <p>
   * This method performs the following steps:
   * 1. Creates a new {@code Entity} instance and sets its subject, location, and policy.
   * 2. Sends a POST request to the `/registry/v1/entities` endpoint to create the entity.
   * 3. Verifies that the response status is HTTP 201 Created, indicating the entity was successfully created.
   * 4. Sends a GET request to the `/registry/v1/entities/{entityId}` endpoint with the subject as an encoded URL parameter.
   * 5. Verifies that the response status is HTTP 200 OK.
   * 6. Validates that the JSON response contains the expected subject value.
   *
   * @throws Exception if an error occurs during the request or validation
   */
  @Test
  public void testGetEntity() throws Exception {
    final PolicyRecord policyRecord = PolicyRecord.builder()
        .name("TestPolicy")
        .policyRecordId(UUID.randomUUID().toString())
        .policy("{\"Test Policy\":\"value\"}")
        .build();
    this.mockMvc.perform(post("/registry/v1/policies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(policyRecord)))
        .andExpect(status().isCreated());

    final EntityRecord record = new EntityRecord();
    final String subject = "https://example.com/subject/2";

    record.setSubject(subject);
    record.setIssuer("http://issuer.swedenconnect.se");
    record.setPolicyRecordId(policyRecord.getPolicyRecordId());
    record.setEntityRecordId(UUID.randomUUID().toString());


    final MvcResult result = this.mockMvc.perform(post("/registry/v1/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(record)))
        .andExpect(status().isCreated()).andReturn();
    final EntityRecord createdRecord = objectMapper.readValue(result.getResponse()
        .getContentAsString(),EntityRecord.class);

    this.mockMvc.perform(get("/registry/v1/entities/{entityId}",createdRecord.getEntityRecordId() ))
        .andExpect(status().isOk())
        //.andExpect(jsonPath("$.entityId").value(subject))
        .andExpect(jsonPath("$.subject").value(subject));
  }

  /**
   * Tests the update of an {@code Entity} via HTTP PUT request.
   * <p>
   * This method performs the following steps:
   * 1. Creates a new {@code Entity} instance and sets its subject, location, and policy.
   * 2. Sends a POST request to the `/registry/v1/entities` endpoint to create the entity.
   * 3. Verifies that the response status is HTTP 201 Created, indicating the entity was successfully created.
   * 4. Updates the location of the {@code Entity}.
   * 5. Sends a PUT request to the `/registry/v1/entities/{entityId}` endpoint with the updated entity data.
   * 6. Verifies that the response status is HTTP 200 OK, indicating the entity was successfully updated.
   * 7. Validates that the JSON response contains the updated location value.
   *
   * @throws Exception if an error occurs during the request or validation
   */
  @Test
  public void testUpdateEntity() throws Exception {
    final PolicyRecord policyRecord = PolicyRecord.builder()
        .name("TestPolicy")
        .policyRecordId(UUID.randomUUID().toString())
        .policy("{\"Test Policy\":\"value\"}")
        .build();
    this.mockMvc.perform(post("/registry/v1/policies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(policyRecord)))
        .andExpect(status().isCreated());

    final EntityRecord record = new EntityRecord();
    final String subject = "https://example.com/subject/3";

    record.setSubject(subject);
    record.setIssuer("http://issuer.swedenconnect.se");
    record.setPolicyRecordId(policyRecord.getPolicyRecordId());
    record.setEntityRecordId(UUID.randomUUID().toString());



    final MvcResult result = this.mockMvc.perform(post("/registry/v1/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(record)))
        .andExpect(status().isCreated()).andReturn();
    final EntityRecord createdRecord = objectMapper.readValue(result.getResponse()
        .getContentAsString(),EntityRecord.class);
    record.setEntityRecordId(createdRecord.getEntityRecordId());

    record.setHostedRecord(EntityRecordHostedRecord.builder().authorityHints(List.of("http://hint1")).build());
    this.mockMvc.perform(put("/registry/v1/entities/{entityId}", createdRecord.getEntityRecordId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(record)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hosted_record.authority_hints").value("http://hint1"));
  }

  /**
   * Tests the deletion of an {@code Entity} via HTTP DELETE request.
   * <p>
   * This method performs the following steps:
   * 1. Generates a random UUID for the entity ID.
   * 2. Creates a new {@code Entity} instance and sets its subject, location, and policy.
   * 3. Sends a POST request to the `/registry/v1/entities` endpoint to create the entity.
   * 4. Verifies that the response status is HTTP 201 Created, indicating the entity was successfully created.
   * 5. Sends a DELETE request to the `/registry/v1/entities/{entityId}` endpoint with the subject as an encoded URL parameter.
   * 6. Verifies that the response status is HTTP 204 No Content, indicating the entity was successfully deleted.
   *
   * @throws Exception if an error occurs during the request or validation
   */
  @Test
  public void testDeleteEntity() throws Exception {
    final PolicyRecord policyRecord = PolicyRecord.builder()
        .name("TestPolicy")
        .policyRecordId(UUID.randomUUID().toString())
        .policy("{\"Test Policy\":\"value\"}")
        .build();
    this.mockMvc.perform(post("/registry/v1/policies")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(policyRecord)))
        .andExpect(status().isCreated());

    final EntityRecord record = new EntityRecord();
    final String subject = "https://example.com/subject/40";

    record.setSubject(subject);
    record.setIssuer("http://issuer.swedenconnect.se");
    record.setPolicyRecordId(policyRecord.getPolicyRecordId());
    record.setEntityRecordId(UUID.randomUUID().toString());


    this.mockMvc.perform(post("/registry/v1/entities")
            .contentType(MediaType.APPLICATION_JSON)
            .content(this.objectMapper.writeValueAsBytes(record)))
        .andExpect(status().isCreated());

    this.mockMvc.perform(delete("/registry/v1/entities/{entityId}", record.getEntityRecordId()))
        .andExpect(status().isNoContent());
  }

  /**
   * Tests the retrieval of all {@code Entity} instances via HTTP GET request.
   * <p>
   * This method performs the following steps:
   * 1. Sends a GET request to the `/registry/v1/entities` endpoint.
   * 2. Verifies that the response status is HTTP 200 OK.
   *
   * @throws Exception if an error occurs during the request or validation
   */
  @Test
  public void testGetAllEntities() throws Exception {
    this.mockMvc.perform(get("/registry/v1/entities"))
        .andExpect(status().isOk());
  }
}