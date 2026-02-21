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
package se.swedenconnect.oidf.registry;

import org.springframework.boot.SpringApplication;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

/**
 * This class serves as the entry point for running the Entity Registry Application with additional configuration
 * specified by TestContainersConfiguration.
 * <p>
 * Starting this class instead of the main {@link RegistryApplication} class invokes testcontainers that are set up in
 * {@link TestContainersConfiguration}.
 * <p>
 * This can be seen as an alternative to running the main {@code EntityRegistryApplication} together with a docker
 * compose setup.
 *
 * @author David Goldring
 * @author Per Fredrik Plars
 * @see <a
 *     href="https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.testcontainers.at-development-time">Testcontainers
 *     at development time</a>
 */

public class TestRegistryApplication {

  /**
   * The main entry point of the TestEntityRegistryApplication.
   *
   * @param args command line arguments passed to the application
   */
  public static void main(String[] args) {
    SpringApplication.from(RegistryApplication::main)
        .with(TestContainersConfiguration.class).run(args);
  }

}
