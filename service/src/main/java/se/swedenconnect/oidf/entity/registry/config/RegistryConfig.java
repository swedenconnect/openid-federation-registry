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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.EntityRepository;
import se.swedenconnect.oidf.entity.registry.entity.EntityService;
import se.swedenconnect.oidf.entity.registry.entity.JpaEntityService;
import se.swedenconnect.oidf.entity.registry.federationserviceapi.FederationApiService;
import se.swedenconnect.oidf.entity.registry.policy.JpaPolicyService;
import se.swedenconnect.oidf.entity.registry.policy.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.policy.PolicyService;
import se.swedenconnect.oidf.entity.registry.trustmark.JpaTrustMarkSubjectService;
import se.swedenconnect.oidf.entity.registry.trustmark.TrustMarkSubjectRepository;
import se.swedenconnect.oidf.entity.registry.trustmark.TrustMarkSubjectService;

import java.text.ParseException;

/**
 * A Spring configuration class that defines beans for different implementations of the EntityService interface.
 *
 * @author David Goldring
 */
@Configuration
public class RegistryConfig {

  private final EntityRepository entityRepository;
  private final PolicyRepository policyRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final RegistryAuditService registryAuditService;
  private final ObjectMapper objectMapper;
  /**
   * Constructs an instance of the RegistryConfig class, initializing its dependencies.
   *
   * @param entityRepository the repository used for accessing and managing entity-related data
   * @param policyRepository the repository used for accessing and managing policy-related data
   * @param trustMarkSubjectRepository the repository used for accessing and managing trustmark subject-related data
   * @param registryAuditService the service used for managing auditing operations in the registry
   * @param objectMapper the object mapper for JSON serialization and deserialization
   */
  public RegistryConfig(final EntityRepository entityRepository, final PolicyRepository policyRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final RegistryAuditService registryAuditService,
      final ObjectMapper objectMapper) {
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.registryAuditService = registryAuditService;
    this.objectMapper = objectMapper;
  }

  /**
   * Provides an instance of JpaEntityService, which implements the EntityService interface. This service manages entity
   * objects using a JPA repository and handles JSON conversion using the provided ObjectMapper.
   *
   * @return an instance of JpaEntityService.
   */
  @Bean
  @Primary
  @Qualifier("jpaEntityService")
  public EntityService jpaEntityService() {
    return new JpaEntityService(
        this.entityRepository,
        this.policyRepository,
        this.objectMapper,
        this.registryAuditService);
  }

  /**
   * Provides an instance of JpaPolicyService, which implements the PolicyService interface. This service manages policy
   * objects using a JPA repository and handles JSON conversion using the provided ObjectMapper.
   *

   * @return an instance of JpaPolicyService.
   */
  @Bean
  @Qualifier("jpaPolicyService")
  public PolicyService jpaPolicyService() {
    return new JpaPolicyService(this.policyRepository, this.objectMapper,this.registryAuditService);
  }

  /**
   * TrustMarkSubjectService
   *
   * @return an instance of TrustMarkSubjectService.
   */
  @Bean
  @Qualifier("jpaTrustMarkSubjectService")
  public TrustMarkSubjectService jpaTrustMarkSubjectService() {
    return new JpaTrustMarkSubjectService(this.trustMarkSubjectRepository, this.objectMapper,this.registryAuditService);
  }

  /**
   * Provides an instance of the FederationApiService for managing federation-related operations.
   *
   * @param registryProperties the registry configuration properties, which include
   *                            settings for the federation service API such as
   *                            signing keys and issuer details.
   * @param mapper the ObjectMapper used for handling JSON serialization
   *               and deserialization.
   * @return an instance of FederationApiService configured with the
   *         necessary dependencies.
   * @throws ParseException if there is an error in parsing
   *                        federation service API configuration properties.
   */
  @Bean
  public FederationApiService federationServiceApiService(final RegistryProperties registryProperties,
                                                          final ObjectMapper mapper)
      throws ParseException {

    final RegistryProperties.FederationAPIProperties federationAPIProperties =
        registryProperties.federationServiceApi();

    return new FederationApiService(
        this.entityRepository,
        federationAPIProperties.federationserviceapiSignKeyJWK(),
        this.policyRepository,
        this.trustMarkSubjectRepository,
        federationAPIProperties.issuer(),
            mapper
    );
  }

}
