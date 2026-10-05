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
 *   <li>A database without a Flyway history table is a fresh installation and gets the consolidated baseline
 *   ({@value #BASELINE}) followed by the migrations in {@value #MIGRATION}.</li>
 *   <li>A database that already has a history table continues with the historical migrations in {@value #LEGACY},
 *   so that a service on, for example, V20 is upgraded step by step, followed by the migrations in
 *   {@value #MIGRATION}.</li>
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

  private static final Logger log = LoggerFactory.getLogger(FlywayLocationsConfiguration.class);

  @Bean
  FlywayConfigurationCustomizer flywayLocationsCustomizer() {
    return configuration -> {
      final boolean existing = hasHistoryTable(configuration.getDataSource(), configuration.getTable());
      if (existing) {
        log.info("Flyway history table found, using legacy migrations");
        configuration.locations(LEGACY, MIGRATION);
      }
      else {
        log.info("No Flyway history table found, using baseline migration");
        configuration.locations(BASELINE, MIGRATION);
      }
    };
  }

  static boolean hasHistoryTable(final DataSource dataSource, final String table) {
    try (final Connection connection = dataSource.getConnection();
        final ResultSet tables = connection.getMetaData()
            .getTables(connection.getCatalog(), connection.getSchema(), table, new String[] { "TABLE" })) {
      return tables.next();
    }
    catch (final SQLException e) {
      throw new IllegalStateException("Could not check for Flyway history table '" + table + "'", e);
    }
  }

}
