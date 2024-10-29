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
package se.swedenconnect.oidf.entity.registry.config;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.service.EntityService;
import se.swedenconnect.oidf.entity.registry.service.impl.FileEntityService;
import se.swedenconnect.oidf.entity.registry.service.impl.JpaEntityService;

/**
 * ServiceConfigTest is a test class for the ServiceConfig Spring configuration class.
 * It uses @AutoConfigureMockMvc to autoconfigure the Spring MVC infrastructure for use
 * in a test environment without starting the full HTTP server.
 *
 * @author David Goldring
 */
@AutoConfigureMockMvc
public class ServiceConfigTest {

  private EntityRepository entityRepository;
  private ServiceConfig serviceConfig;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Sets up the testing environment before each test execution.
   * This method initializes and mocks dependencies such as EntityRepository and creates
   * a new instance of ServiceConfig with the mocked repository.
   */
  @BeforeEach
  public void setUp() {
    entityRepository = mock(EntityRepository.class);
    serviceConfig = new ServiceConfig(entityRepository);
  }

  /**
   * Tests the fileEntityService bean provided by the ServiceConfig.
   * <p>
   * The test verifies that the fileEntityService bean is not null and
   * confirms that it is an instance of FileEntityService class.
   */
  @Test
  public void testFileEntityServiceBean() {
    EntityService fileService = serviceConfig.fileEntityService();
    assertThat(fileService).isNotNull();
    assertThat(fileService).isInstanceOf(FileEntityService.class);
  }

  /**
   * Tests the jpaEntityService bean provided by the ServiceConfig.
   * <p>
   * The test verifies that the jpaEntityService bean is not null, confirms
   * that it is an instance of the JpaEntityService class, and
   * asserts that the JpaEntityService is configured with the correct repository.
   */
  @Test
  public void testJpaEntityServiceBean() {
    EntityService jpaService = serviceConfig.jpaEntityService(this.objectMapper);
    assertThat(jpaService).isNotNull();
    assertThat(jpaService).isInstanceOf(JpaEntityService.class);
    assertSame(entityRepository, ((JpaEntityService) jpaService).getRepository());
  }
}