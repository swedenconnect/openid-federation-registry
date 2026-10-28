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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.MariaDBContainer;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

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

  /** One server, one database per migration path. */
  @Container
  static MariaDBContainer<?> server = new MariaDBContainer<>(TestContainersConfiguration.MARIADB_VERSION);

  private static final String BASELINE_DB = "baseline";
  private static final String LEGACY_DB = "legacy";
  private static final String UPGRADED_DB = "upgraded";
  private static final String RESTARTED_DB = "restarted";
  private static final String LEGACY_RESTARTED_DB = "legacy_restarted";

  @BeforeAll
  static void createDatabases() throws SQLException {
    try (final Connection c = DriverManager.getConnection(server.getJdbcUrl(), "root", server.getPassword());
        final var st = c.createStatement()) {
      for (final String db : List.of(BASELINE_DB, LEGACY_DB, UPGRADED_DB, RESTARTED_DB, LEGACY_RESTARTED_DB)) {
        st.execute("create database " + db);
      }
    }
  }

  @Test
  void restartAfterBaselineInstallKeepsUsingBaseline() {
    final String table = "flyway_schema_history";
    assertThat(FlywayLocationsConfiguration.isLegacyInstallation(dataSource(RESTARTED_DB), table)).isFalse();
    flyway(RESTARTED_DB, null, FlywayLocationsConfiguration.BASELINE).migrate();

    // Second node / restart: the history table now exists but holds only the baseline.
    assertThat(FlywayLocationsConfiguration.isLegacyInstallation(dataSource(RESTARTED_DB), table)).isFalse();
    final Flyway second = flyway(RESTARTED_DB, null, FlywayLocationsConfiguration.BASELINE);
    second.validate();
    assertThat(second.migrate().migrationsExecuted).isZero();
  }

  @Test
  void databaseWithPreBaselineHistoryIsLegacy() {
    final String table = "flyway_schema_history";
    flyway(LEGACY_RESTARTED_DB, "20", FlywayLocationsConfiguration.LEGACY).migrate();
    assertThat(FlywayLocationsConfiguration.isLegacyInstallation(dataSource(LEGACY_RESTARTED_DB), table)).isTrue();
  }

  @Test
  void freshInstallAndLegacyPathProduceSameSchema() throws SQLException {
    final String table = "flyway_schema_history";
    assertThat(FlywayLocationsConfiguration.hasHistoryTable(dataSource(BASELINE_DB), table)).isFalse();
    flyway(BASELINE_DB, null, FlywayLocationsConfiguration.BASELINE).migrate();
    assertThat(FlywayLocationsConfiguration.hasHistoryTable(dataSource(BASELINE_DB), table)).isTrue();

    flyway(LEGACY_DB, null, FlywayLocationsConfiguration.LEGACY).migrate();

    assertThat(describeSchema(BASELINE_DB)).isNotEmpty().isEqualTo(describeSchema(LEGACY_DB));
  }

  @Test
  void databaseOnV20IsUpgradedThroughLegacyMigrations() throws SQLException {
    flyway(UPGRADED_DB, "20", FlywayLocationsConfiguration.LEGACY).migrate();
    assertThat(currentVersion(UPGRADED_DB)).isEqualTo("20");

    flyway(UPGRADED_DB, null, FlywayLocationsConfiguration.LEGACY).migrate();

    assertThat(currentVersion(UPGRADED_DB)).isEqualTo(currentVersion(BASELINE_DB));
  }

  private static String currentVersion(final String db) {
    return flyway(db, null, FlywayLocationsConfiguration.LEGACY).info().current().getVersion().getVersion();
  }

  private static Flyway flyway(final String db, final String target, final String location) {
    return Flyway.configure()
        .dataSource(url(db), "root", server.getPassword())
        .locations(location, FlywayLocationsConfiguration.MIGRATION)
        .target(target == null ? "latest" : target)
        .load();
  }

  private static String url(final String db) {
    return "jdbc:mariadb://" + server.getHost() + ":" + server.getMappedPort(3306) + "/" + db;
  }

  private static DataSource dataSource(final String db) {
    return new org.springframework.jdbc.datasource.DriverManagerDataSource(url(db), "root", server.getPassword());
  }

  /** Columns, indexes and constraints as seen by information_schema, one string per row. */
  private static List<String> describeSchema(final String db) throws SQLException {
    final List<String> rows = new ArrayList<>();
    try (final Connection c = DriverManager.getConnection(url(db), "root", server.getPassword())) {
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
