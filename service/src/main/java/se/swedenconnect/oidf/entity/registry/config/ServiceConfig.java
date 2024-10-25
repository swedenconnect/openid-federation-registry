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
import se.swedenconnect.oidf.entity.registry.repository.EntityRepository;
import se.swedenconnect.oidf.entity.registry.service.EntityService;
import se.swedenconnect.oidf.entity.registry.service.impl.FileEntityService;
import se.swedenconnect.oidf.entity.registry.service.impl.JpaEntityService;

/**
 * A Spring configuration class that defines beans for different implementations of the EntityService interface.
 *
 * @author David Goldring
 */
@Configuration
public class ServiceConfig {

  private final EntityRepository entityRepository;

  /**
   * Constructs a new ServiceConfig with the specified EntityRepository.
   *
   * @param entityRepository the repository used by the JpaEntityService for CRUD operations.
   */
  public ServiceConfig(final EntityRepository entityRepository) {
    this.entityRepository = entityRepository;
  }

  /**
   * Provides an instance of FileEntityService, which implements the EntityService interface.
   * This service manages entity objects using a file-based storage mechanism.
   *
   * @return an instance of FileEntityService.
   */
  @Bean
  @Qualifier("fileEntityService")
  public EntityService fileEntityService() {
    return new FileEntityService();
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
}
