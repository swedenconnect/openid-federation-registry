/*
 * Copyright 2026 Sweden Connect
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
package se.swedenconnect.oidf.registry.infrastructure.config;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import se.swedenconnect.oidf.registry.infrastructure.audit.AuditEventLogListener;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditEventPublisher;
import se.swedenconnect.oidf.registry.infrastructure.audit.RegistryAuditService;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

/**
 * Configuration for auditing metadata.
 *
 * @author Per Fredrik Plars
 */
@Configuration
public class AuditingConfig {

  /**
   * Provides an instance of {@link AuditorAware} that identifies the current auditor for auditing purposes. This
   * implementation returns a predefined user ("systemUser") as the auditor. The logic for fetching the actual
   * authenticated user's details should be implemented to replace the placeholder.
   *
   * @return an {@link AuditorAware} instance that provides the current auditor's identifier as a string.
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> {
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated()) {
        return Optional.ofNullable(authentication.getName());
      }
      return Optional.empty();
    };
  }

  /**
   * Creates a {@link RegistryAuditService} bean that provides audit logging functionality for actions performed within
   * the Federation API. This method configures and returns an instance of {@link RegistryAuditEventPublisher}, which
   * implements the {@link RegistryAuditService} interface.
   *
   * @param publisher the {@link ApplicationEventPublisher} used for publishing audit events.
   * @param currentUser the {@link AuditorAware} implementation to provide information about the current auditor.
   * @param objectMapper the {@link JsonMapper} instance for handling JSON serialization and deserialization.
   * @return an instance of {@link RegistryAuditEventPublisher} configured with the provided parameters.
   */
  @Bean
  public RegistryAuditService registryAuditService(final ApplicationEventPublisher publisher,
      final AuditorAware<String> currentUser,
      final JsonMapper objectMapper) {
    return new RegistryAuditEventPublisher(publisher, currentUser, objectMapper);
  }

  /**
   * Creates and returns a new instance of {@link AuditEventLogListener}, a listener that processes and logs audit
   * events based on the configured log level.
   *
   * @return an instance of {@link AuditEventLogListener}.
   */
  @Bean
  public AuditEventLogListener auditEventLogListener() {
    return new AuditEventLogListener();
  }

  /**
   * Provides an instance of {@link InMemoryAuditEventRepository} for storing audit events in-memory. This bean will
   * only be created if no other {@link AuditEventRepository} bean is defined in the application context.
   *
   * @return an instance of {@link InMemoryAuditEventRepository}.
   */
  @Bean
  @ConditionalOnMissingBean({ AuditEventRepository.class })
  InMemoryAuditEventRepository auditEventRepository() {
    return new InMemoryAuditEventRepository();
  }

}