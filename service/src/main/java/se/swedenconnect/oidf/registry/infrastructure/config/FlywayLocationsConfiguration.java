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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Chooses which Flyway migrations to run.
 * <ul>
 *   <li>A database without a Flyway history table, or one whose history starts at the baseline (no applied version
 *   below {@value #BASELINE_VERSION}), gets the consolidated baseline ({@value #BASELINE}) followed by the migrations
 *   in {@value #MIGRATION}. This keeps a second node, or a restart, on the same path as the first start.</li>
 *   <li>A database whose history contains a version below {@value #BASELINE_VERSION} continues with the historical
 *   migrations in {@value #LEGACY}, so that a service on, for example, V20 is upgraded step by step, followed by the
 *   migrations in {@value #MIGRATION}.</li>
 * </ul>
 * The baseline and the last legacy migration have the same version, which means that both paths end up with the same
 * schema and the same version in the history table.
 *
 * @author Per Fredrik Plars
 */
@Configuration
public class FlywayLocationsConfiguration {

  static final String BASELINE = "classpath:db/baseline";
  static final String LEGACY = "classpath:db/legacy";
  static final String MIGRATION = "classpath:db/migration";
  static final int BASELINE_VERSION = 28;

  private static final Logger log = LoggerFactory.getLogger(FlywayLocationsConfiguration.class);

  @Bean
  FlywayConfigurationCustomizer flywayLocationsCustomizer() {
    return configuration -> {
      final boolean legacy = isLegacyInstallation(configuration.getDataSource(), configuration.getTable());
      if (legacy) {
        log.info("Flyway history contains versions before {}, using legacy migrations", BASELINE_VERSION);
        configuration.locations(LEGACY, MIGRATION);
      }
      else {
        log.info("No Flyway history before {} found, using baseline migration", BASELINE_VERSION);
        configuration.locations(BASELINE, MIGRATION);
      }
    };
  }

  /**
   * Tells whether the database was created by the historical migrations, i.e. its history table holds an applied
   * version lower than the baseline version.
   *
   * @param dataSource the data source
   * @param table the name of the Flyway history table
   * @return true if the historical migrations should be used
   */
  static boolean isLegacyInstallation(final DataSource dataSource, final String table) {
    try (final Connection connection = dataSource.getConnection()) {
      if (!hasHistoryTable(connection, table)) {
        return false;
      }
      final String quote = connection.getMetaData().getIdentifierQuoteString().trim();
      final String sql = "select 1 from " + quote + table + quote
          + " where version is not null and cast(version as decimal(20,6)) < " + BASELINE_VERSION;
      try (final var statement = connection.createStatement(); final ResultSet rs = statement.executeQuery(sql)) {
        return rs.next();
      }
    }
    catch (final SQLException e) {
      throw new IllegalStateException("Could not read Flyway history table '" + table + "'", e);
    }
  }

  static boolean hasHistoryTable(final DataSource dataSource, final String table) {
    try (final Connection connection = dataSource.getConnection()) {
      return hasHistoryTable(connection, table);
    }
    catch (final SQLException e) {
      throw new IllegalStateException("Could not check for Flyway history table '" + table + "'", e);
    }
  }

  private static boolean hasHistoryTable(final Connection connection, final String table) throws SQLException {
    try (final ResultSet tables = connection.getMetaData()
        .getTables(connection.getCatalog(), connection.getSchema(), table, new String[] { "TABLE" })) {
      return tables.next();
    }
  }

}
