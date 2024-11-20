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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import se.swedenconnect.oidf.entity.registry.entity.EntityRepository;
import se.swedenconnect.oidf.entity.registry.federationserviceapi.FederationServiceApiService;
import se.swedenconnect.oidf.entity.registry.policy.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.entity.EntityService;
import se.swedenconnect.oidf.entity.registry.policy.PolicyService;
import se.swedenconnect.oidf.entity.registry.entity.JpaEntityService;
import se.swedenconnect.oidf.entity.registry.policy.JpaPolicyService;
import se.swedenconnect.oidf.entity.registry.trustmark.TrustMarkSubjectRepository;

import java.text.ParseException;

/**
 * A Spring configuration class that defines beans for different implementations of the EntityService interface.
 *
 * @author David Goldring
 */
@Configuration
public class ServiceConfig {

  private final EntityRepository entityRepository;
  private final PolicyRepository policyRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;

  /**
   * Constructs a new ServiceConfig with the specified repositories.
   *
   * @param entityRepository the {@link EntityRepository} used for accessing and performing CRUD operations on entities
   * @param policyRepository the {@link PolicyRepository} used for accessing and performing CRUD operations on policies
   * @param trustMarkSubjectRepository the {@link TrustMarkSubjectRepository}
   * used for accessing and performing CRUD operations on policies
   */
  public ServiceConfig(final EntityRepository entityRepository, final PolicyRepository policyRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository) {
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
  }

  /**
   * Provides an instance of JpaEntityService, which implements the EntityService interface.
   * This service manages entity objects using a JPA repository and handles JSON conversion
   * using the provided ObjectMapper.
   *
   * @param objectMapper the ObjectMapper used for JSON conversion between Entity objects and their DAO representations.
   * @return an instance of JpaEntityService.
   */
  @Bean
  @Primary
  @Qualifier("jpaEntityService")
  public EntityService jpaEntityService(final ObjectMapper objectMapper) {
    return new JpaEntityService(this.entityRepository, objectMapper);
  }

  /**
   * Provides an instance of JpaPolicyService, which implements the PolicyService interface.
   * This service manages policy objects using a JPA repository and handles JSON conversion
   * using the provided ObjectMapper.
   *
   * @param objectMapper the ObjectMapper used for JSON conversion between Policy objects and their DAO representations.
   * @return an instance of JpaPolicyService.
   */
  @Bean
  @Qualifier("jpaPolicyService")
  public PolicyService jpaPolicyService(final ObjectMapper objectMapper) {
    return new JpaPolicyService(this.policyRepository, objectMapper);
  }


  @Bean
  public FederationServiceApiService federationServiceApiService(RegistryProperties registryProperties)
      throws ParseException {
      return new FederationServiceApiService(entityRepository,
          registryProperties.federationserviceapiSignKeyJWK(),
          policyRepository,trustMarkSubjectRepository);
  }

}
