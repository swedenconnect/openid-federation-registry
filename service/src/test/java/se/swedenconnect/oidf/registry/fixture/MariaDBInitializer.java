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

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MariaDBContainer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fixture class for setting up and starting a MariaDB test container instance.
 * <br>
 * Usage:<br> - Can be used directly by annotating your test class with {@code @ExtendWith(MariaDBInitializer.class)}.
 * But for a more declarative approach use the custom marker annotation {@link UseMariaDBContainer}
 *
 * @author David Goldring
 */
public class MariaDBInitializer implements BeforeAllCallback {

  private static final AtomicBoolean started = new AtomicBoolean(false);

  private final MariaDBContainer<?> mariaDB = new TestContainersConfiguration().mariaDBContainer();

  @Override
  public void beforeAll(final ExtensionContext extensionContext) {
    if (started.compareAndSet(false, true)) {
      this.mariaDB.start();
      this.loadProperties();
    }
  }

  /**
   * Loads the properties required for configuring the database connection and the Flyway migration tool. These
   * properties are retrieved from the {@code MariaDBContainer} instance managed by this class.
   */
  private void loadProperties() {
    String jdbcUrl = this.mariaDB.getJdbcUrl();

    System.setProperty("spring.datasource.url", jdbcUrl);
    System.setProperty("spring.datasource.username", this.mariaDB.getUsername());
    System.setProperty("spring.datasource.password", this.mariaDB.getPassword());

    System.setProperty("spring.flyway.url", jdbcUrl);
    System.setProperty("spring.flyway.user", this.mariaDB.getUsername());
    System.setProperty("spring.flyway.password", this.mariaDB.getPassword());
  }
}
