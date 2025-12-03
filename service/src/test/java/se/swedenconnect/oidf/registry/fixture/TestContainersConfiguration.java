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
package se.swedenconnect.oidf.registry.fixture;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;

/**
 * Test configuration class for setting up Testcontainers for the application. This configuration class should contain
 * the necessary bean definitions to initialize and configure Testcontainers for integration tests.
 *
 * @author David Goldring
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

  public static final String MARIADB_VERSION = "mariadb:11.7";
  /**
   * Creates a MariaDBContainer bean.
   *
   * @return a configured MariaDBContainer instance
   */
  @Bean
  @ServiceConnection
    public MariaDBContainer<?> mariaDBContainer() {
    return new MariaDBContainer<>(MARIADB_VERSION);
  }
}
