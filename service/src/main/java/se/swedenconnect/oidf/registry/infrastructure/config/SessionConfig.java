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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.MySqlJdbcIndexedSessionRepositoryCustomizer;

/**
 * Configuration of the GUI sessions that Spring Session stores in the database.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Configuration
public class SessionConfig {

  /**
   * Customizes the session repository in two ways.
   * <ul>
   *   <li>It uses the MySQL queries for the session tables. Spring Boot keeps the generic queries, where storing an
   *   attribute that another request has already stored fails with a duplicate key.</li>
   *   <li>An attribute that can no longer be read, for example one stored by another version of the service, is read
   *   as missing. The user is then no longer logged in and is sent to the login page, instead of every request
   *   failing.</li>
   * </ul>
   *
   * @return the customizer of the session repository
   */
  @Bean
  public SessionRepositoryCustomizer<JdbcIndexedSessionRepository> sessionRepositoryCustomizer() {
    final MySqlJdbcIndexedSessionRepositoryCustomizer mySql = new MySqlJdbcIndexedSessionRepositoryCustomizer();
    return repository -> {
      mySql.customize(repository);
      repository.setConversionService(conversionService());
    };
  }

  static GenericConversionService conversionService() {
    final DeserializingConverter deserializer = new DeserializingConverter(SessionConfig.class.getClassLoader());
    final GenericConversionService conversionService = new GenericConversionService();
    conversionService.addConverter(Object.class, byte[].class, new SerializingConverter());
    conversionService.addConverter(byte[].class, Object.class, source -> {
      try {
        return deserializer.convert(source);
      }
      catch (final SerializationFailedException e) {
        log.info("A session attribute could not be read and is ignored: {}", e.getMessage());
        return null;
      }
    });
    return conversionService;
  }
}
