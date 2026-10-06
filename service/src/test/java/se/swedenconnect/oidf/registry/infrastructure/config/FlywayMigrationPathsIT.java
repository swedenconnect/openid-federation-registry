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

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.MariaDBContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies that a fresh installation (baseline) and an installation migrated from the historical migrations end up
 * with the same schema.
 */
@Testcontainers
class FlywayMigrationPathsIT {

  @Container
  static MariaDBContainer<?> baseline = new MariaDBContainer<>("mariadb:11.7");

  @Container
  static MariaDBContainer<?> legacy = new MariaDBContainer<>("mariadb:11.7");

  @Container
  static MariaDBContainer<?> upgraded = new MariaDBContainer<>("mariadb:11.7");

  @Test
  void freshInstallAndLegacyPathProduceSameSchema() throws SQLException {
    assertThat(FlywayLocationsConfiguration.hasHistoryTable(dataSource(baseline), "flyway_schema_history")).isFalse();
    flyway(baseline, null, FlywayLocationsConfiguration.BASELINE).migrate();
    assertThat(FlywayLocationsConfiguration.hasHistoryTable(dataSource(baseline), "flyway_schema_history")).isTrue();

    flyway(legacy, null, FlywayLocationsConfiguration.LEGACY).migrate();

    assertThat(describeSchema(baseline)).isNotEmpty().isEqualTo(describeSchema(legacy));
  }

  @Test
  void databaseOnV20IsUpgradedThroughLegacyMigrations() throws SQLException {
    flyway(upgraded, "20", FlywayLocationsConfiguration.LEGACY).migrate();
    assertThat(currentVersion(upgraded)).isEqualTo("20");

    flyway(upgraded, null, FlywayLocationsConfiguration.LEGACY).migrate();

    assertThat(currentVersion(upgraded)).isEqualTo(currentVersion(baseline));
  }

  private static String currentVersion(final MariaDBContainer<?> db) {
    return flyway(db, null, FlywayLocationsConfiguration.LEGACY).info().current().getVersion().getVersion();
  }

  private static Flyway flyway(final MariaDBContainer<?> db, final String target, final String location) {
    return Flyway.configure()
        .dataSource(db.getJdbcUrl(), db.getUsername(), db.getPassword())
        .locations(location, FlywayLocationsConfiguration.MIGRATION)
        .target(target == null ? "latest" : target)
        .load();
  }

  private static DataSource dataSource(final MariaDBContainer<?> db) {
    return new org.springframework.jdbc.datasource.DriverManagerDataSource(db.getJdbcUrl(), db.getUsername(),
        db.getPassword());
  }

  /** Columns, indexes and constraints as seen by information_schema, one string per row. */
  private static List<String> describeSchema(final MariaDBContainer<?> db) throws SQLException {
    final List<String> rows = new ArrayList<>();
    try (final Connection c = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword())) {
      query(c, """
          select concat_ws('|', 'col', table_name, column_name, column_type, is_nullable, column_default,
                 generation_expression, extra, column_comment)
          from information_schema.columns where table_schema = database()
            and table_name <> 'flyway_schema_history'""", rows);
      query(c, """
          select concat_ws('|', 'idx', table_name, index_name, non_unique, seq_in_index, column_name)
          from information_schema.statistics where table_schema = database()
            and table_name <> 'flyway_schema_history'""", rows);
      query(c, """
          select concat_ws('|', 'fk', constraint_name, table_name, referenced_table_name, update_rule, delete_rule)
          from information_schema.referential_constraints where constraint_schema = database()""", rows);
      query(c, """
          select concat_ws('|', 'tc', table_name, constraint_name, constraint_type)
          from information_schema.table_constraints where table_schema = database()
            and table_name <> 'flyway_schema_history'""", rows);
    }
    rows.sort(String::compareTo);
    return rows;
  }

  private static void query(final Connection c, final String sql, final List<String> out) throws SQLException {
    try (final var st = c.createStatement(); final var rs = st.executeQuery(sql)) {
      while (rs.next()) {
        out.add(rs.getString(1));
      }
    }
  }
}
