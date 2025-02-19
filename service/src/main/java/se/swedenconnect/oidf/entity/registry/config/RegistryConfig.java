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
package se.swedenconnect.oidf.entity.registry.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.OrganizationRepository;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;
import se.swedenconnect.oidf.entity.registry.repository.TrustMarkSubjectRepository;
import se.swedenconnect.oidf.entity.registry.service.EntityService;
import se.swedenconnect.oidf.entity.registry.service.FederationApiService;
import se.swedenconnect.oidf.entity.registry.service.JpaEntityService;
import se.swedenconnect.oidf.entity.registry.service.JpaPolicyService;
import se.swedenconnect.oidf.entity.registry.service.JpaTrustMarkSubjectService;
import se.swedenconnect.oidf.entity.registry.service.OptionsCRUDTrustMark;
import se.swedenconnect.oidf.entity.registry.service.PolicyService;
import se.swedenconnect.oidf.entity.registry.service.TrustMarkSubjectService;
import se.swedenconnect.security.credential.PkiCredential;
import se.swedenconnect.security.credential.bundle.CredentialBundles;
import se.swedenconnect.security.credential.nimbus.JwkTransformerFunction;

import java.time.Duration;

/**
 * A Spring configuration class that defines beans for different implementations of the EntityService interface.
 *
 * @author Per Fredrik Plars
 * @author David Goldring
 */
@Configuration
public class RegistryConfig {

  private final EntityRepository entityRepository;
  private final PolicyRepository policyRepository;
  private final TrustMarkSubjectRepository trustMarkSubjectRepository;
  private final RegistryAuditService registryAuditService;
  final InstanceRepository instanceRepository;
  private final ObjectMapper objectMapper;

  /**
   * Constructs an instance of the RegistryConfig class with the specified dependencies.
   *
   * @param entityRepository the repository used for managing entity-related data in the database.
   * @param policyRepository the repository used for managing policy-related data in the database.
   * @param trustMarkSubjectRepository the repository used for managing trustmark subject-related data.
   * @param registryAuditService the service used for auditing actions and events within the registry.
   * @param objectMapper the object mapper used for JSON serialization and deserialization.
   * @param instanceRepository the repository used for managing instance-related data in the registry.
   */
  public RegistryConfig(final EntityRepository entityRepository, final PolicyRepository policyRepository,
      final TrustMarkSubjectRepository trustMarkSubjectRepository,
      final RegistryAuditService registryAuditService,
      final ObjectMapper objectMapper,
      final InstanceRepository instanceRepository) {
    this.entityRepository = entityRepository;
    this.policyRepository = policyRepository;
    this.trustMarkSubjectRepository = trustMarkSubjectRepository;
    this.registryAuditService = registryAuditService;
    this.objectMapper = objectMapper;
    this.instanceRepository = instanceRepository;
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
   * Provides an instance of the {@link PolicyService} implementation using JPA for managing JSON Policy objects.
   *
   * @param organizationRepository the repository used for managing organization-related data, required to associate
   *     policies with respective organizations.
   * @return an instance of {@link JpaPolicyService}, configured with the necessary dependencies such as
   *     {@code policyRepository}, {@code objectMapper}, and {@code registryAuditService}.
   */
  @Bean
  @Qualifier("jpaPolicyService")
  public PolicyService jpaPolicyService(final OrganizationRepository organizationRepository) {
    return new JpaPolicyService(this.policyRepository, this.objectMapper, this.registryAuditService,
        organizationRepository);
  }

  /**
   * TrustMarkSubjectService
   *
   * @return an instance of TrustMarkSubjectService.
   */
  @Bean
  @Qualifier("jpaTrustMarkSubjectService")
  public TrustMarkSubjectService jpaTrustMarkSubjectService() {
    return new JpaTrustMarkSubjectService(this.trustMarkSubjectRepository, this.objectMapper,
        this.registryAuditService);
  }

  /**
   * Provides an instance of the FederationApiService for managing federation-related operations.
   *
   * @param registryProperties the registry configuration properties, which include settings for the federation
   *     service API such as signing keys and issuer details.
   * @param mapper the ObjectMapper used for handling JSON serialization and deserialization.
   * @param credentialBundles Bundle for reading keys
   * @param trustMarkService TrustmarkCRUD service
   * @return an instance of FederationApiService configured with the necessary dependencies.
   */
  @Bean
  public FederationApiService federationServiceApiService(final RegistryProperties registryProperties,
      final CredentialBundles credentialBundles, final OptionsCRUDTrustMark trustMarkService,
      final ObjectMapper mapper) {

    final RegistryProperties.FederationAPIProperties federationAPIProperties =
        registryProperties.federationServiceApi();

    final String signKeyAlias = federationAPIProperties.signKeyAlias();
    final Duration tokenExpiryDuration = federationAPIProperties.tokenExpiryDuration();

    final PkiCredential signKey = credentialBundles.getCredential(signKeyAlias);
    final JwkTransformerFunction function = new JwkTransformerFunction();
    final JWK jwk = function.apply(signKey);

    return new FederationApiService(
        this.entityRepository,
        jwk,
        this.policyRepository,
        this.trustMarkSubjectRepository,
        federationAPIProperties.issuer(),
        mapper,
        this.instanceRepository,
        trustMarkService,
        tokenExpiryDuration
    );
  }

  /**
   * Initializes instances by converting the provided registry properties into entity objects and persisting them to the
   * database.
   *
   * @param registryProperties the registry configuration properties containing instance information that will be
   *     used to populate and store {@code InstanceEntity} objects.
   */
  @Autowired
  void initInstance(final RegistryProperties registryProperties) {

    registryProperties.instances()
        .forEach(instance -> {
          final InstanceEntity entity = new InstanceEntity();
          entity.setInstanceId(instance.instanceId());
          entity.setName(instance.name());
          entity.setCreatedBy("Registry-Config");
          entity.setLastModifiedBy(entity.getCreatedBy());
          this.instanceRepository.saveAndFlush(entity);

        });
  }

}
