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
package se.swedenconnect.oidf.registry.fixture;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.testcontainers.containers.MariaDBContainer;

import javax.sql.DataSource;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

/**
 * Empties the MariaDB database of {@link TestContainersConfiguration} before each test class that reuses a cached
 * application context.
 * <p>
 * The integration tests share one container and one context per distinct configuration, but they are written for a
 * database of their own: they use the same organizations, and only one organization may hold the domain
 * {@code localhost}. The tables are therefore truncated before the next test class runs. The instances, which the
 * registry creates when the context starts, and the Flyway history are kept.
 * <p>
 * Registered in {@code META-INF/spring.factories}, and does nothing for contexts without the MariaDB container.
 *
 * @author Per Fredrik Plars
 */
public class CleanDatabaseTestExecutionListener extends AbstractTestExecutionListener {

  private static final Set<String> KEPT_TABLES = Set.of("flyway_schema_history", "instance");

  @Override
  public void beforeTestClass(final TestContext testContext) throws Exception {
    // A context that is not loaded yet comes with a new, empty container.
    if (!testContext.hasApplicationContext()
        || testContext.getApplicationContext().getBeanNamesForType(MariaDBContainer.class).length == 0) {
      return;
    }
    final DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);
    final List<String> tables = new JdbcTemplate(dataSource).queryForList(
        "select table_name from information_schema.tables"
            + " where table_schema = database() and table_type = 'BASE TABLE'",
        String.class);
    try (var connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("set foreign_key_checks = 0");
      for (final String table : tables) {
        if (!KEPT_TABLES.contains(table)) {
          statement.execute("truncate table `" + table + "`");
        }
      }
      statement.execute("set foreign_key_checks = 1");
    }
  }
}
