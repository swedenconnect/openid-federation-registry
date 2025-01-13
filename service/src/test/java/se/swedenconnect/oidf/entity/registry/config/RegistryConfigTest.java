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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditLogger;
import se.swedenconnect.oidf.entity.registry.entity.EntityRepository;
import se.swedenconnect.oidf.entity.registry.entity.EntityService;
import se.swedenconnect.oidf.entity.registry.entity.JpaEntityService;
import se.swedenconnect.oidf.entity.registry.policy.JpaPolicyService;
import se.swedenconnect.oidf.entity.registry.policy.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.policy.PolicyService;
import se.swedenconnect.oidf.entity.registry.trustmark.TrustMarkSubjectRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * ServiceConfigTest is a test class for the ServiceConfig Spring configuration class.
 * It uses @AutoConfigureMockMvc to autoconfigure the Spring MVC infrastructure for use
 * in a test environment without starting the full HTTP server.
 *
 * @author David Goldring
 */
@AutoConfigureMockMvc
public class RegistryConfigTest {

  private EntityRepository entityRepository;
  private PolicyRepository policyRepository;
  private TrustMarkSubjectRepository trustMarkSubjectRepository;
  private RegistryConfig registryConfig;

  /**
   * Sets up the testing environment before each test execution.
   * This method initializes and mocks dependencies such as EntityRepository and creates
   * a new instance of ServiceConfig with the mocked repository.
   */
  @BeforeEach
  public void setUp() {
    entityRepository = mock(EntityRepository.class);
    policyRepository = mock(PolicyRepository.class);
    trustMarkSubjectRepository = mock(TrustMarkSubjectRepository.class);
    registryConfig = new RegistryConfig(entityRepository, policyRepository,trustMarkSubjectRepository,
        new RegistryAuditLogger(),
        new ObjectMapper());
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
    EntityService jpaService = registryConfig.jpaEntityService();
    assertThat(jpaService).isNotNull();
    assertThat(jpaService).isInstanceOf(JpaEntityService.class);
    assertSame(entityRepository, ((JpaEntityService) jpaService).getRepository());
  }

  /**
   * Tests the jpaPolicyService bean provided by the ServiceConfig.
   * <p>
   * The test verifies that the jpaPolicyService bean is not null, confirms
   * that it is an instance of the JpaPolicyService class, and
   * asserts that the JpaPolicyService is configured with the correct repository.
   */
  @Test
  public void testJpaPolicyServiceBean() {
    PolicyService policyService = registryConfig.jpaPolicyService();
    assertThat(policyService).isNotNull();
    assertThat(policyService).isInstanceOf(JpaPolicyService.class);
    assertSame(policyRepository, ((JpaPolicyService) policyService).getPolicyRepository());
  }
}