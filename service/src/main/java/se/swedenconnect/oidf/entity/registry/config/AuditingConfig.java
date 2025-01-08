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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import se.swedenconnect.oidf.entity.registry.audit.RegistryAuditService;

import java.util.Optional;
import java.util.UUID;

/**
 * Configuration for auditing metadata.
 *
 *  @author Per Fredrik Plars
 */
@Configuration
public class AuditingConfig {

  /**
   * Provides an instance of {@link AuditorAware} that identifies the current auditor for auditing purposes.
   * This implementation returns a predefined user ("systemUser") as the auditor. The logic for fetching
   * the actual authenticated user's details should be implemented to replace the placeholder.
   *
   * @return an {@link AuditorAware} instance that provides the current auditor's identifier as a string.
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
/*
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      return Optional.of(authentication.getName()); // Autentiserad användare
    }
    return Optional.empty();

 */
    return () -> Optional.of("DefaultSystemUser:" + UUID.randomUUID());
  }

  @Bean
  public RegistryAuditService registryAuditService(ApplicationEventPublisher publisher){
    return new RegistryAuditService(publisher);
  }


}