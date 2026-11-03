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
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.swedenconnect.oidf.registry.fixture.TestContainersConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verifies that V37 moves the trust mark token validity duration from the trust mark issuer to its trust marks.
 */
@Testcontainers
class TrustmarkValidityDurationMigrationIT {

  @Container
  static MariaDBContainer<?> server = new MariaDBContainer<>(TestContainersConfiguration.MARIADB_VERSION);

  private static final Timestamp LAST_MODIFIED = Timestamp.valueOf("2026-01-02 03:04:05");

  @Test
  void eachTrustmarkGetsTheDurationOfItsIssuer() throws SQLException {
    this.migrate("36");

    final UUID twoDays = UUID.randomUUID();
    final UUID forever = UUID.randomUUID();
    final UUID first = UUID.randomUUID();
    final UUID second = UUID.randomUUID();
    final UUID third = UUID.randomUUID();
    try (final Connection c = this.connect(); final var st = c.createStatement()) {
      // Only the two tables that the migration touches are filled, the entities they point at are left out.
      st.execute("SET FOREIGN_KEY_CHECKS = 0");
      st.execute(issuer(twoDays, "'P2D'"));
      st.execute(issuer(forever, "NULL"));
      st.execute(trustmark(first, twoDays, "https://example.com/tm/one"));
      st.execute(trustmark(second, twoDays, "https://example.com/tm/two"));
      st.execute(trustmark(third, forever, "https://example.com/tm/three"));
    }

    this.migrate(null);

    final Map<UUID, String> durations = new HashMap<>();
    try (final Connection c = this.connect(); final var st = c.createStatement();
        final var rs = st.executeQuery(
            "SELECT trustmark_id, trust_mark_token_validity_duration, last_modified_date FROM trustmark")) {
      while (rs.next()) {
        durations.put(UUID.fromString(rs.getString(1)), rs.getString(2));
        assertThat(rs.getTimestamp(3)).isEqualTo(LAST_MODIFIED);
      }
    }
    assertThat(durations).containsEntry(first, "P2D").containsEntry(second, "P2D").containsEntry(third, null);

    try (final Connection c = this.connect(); final var st = c.createStatement();
        final var rs = st.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
            + "WHERE table_schema = DATABASE() AND table_name = 'trustmark_issuer' "
            + "AND column_name = 'trust_mark_token_validity_duration'")) {
      rs.next();
      assertThat(rs.getInt(1)).isZero();
    }
  }

  private static String issuer(final UUID id, final String duration) {
    return ("INSERT INTO trustmark_issuer (trustmark_issuer_id, entity_id, active, trust_mark_token_validity_duration, "
        + "created_by, last_modified_by) VALUES ('%s', '%s', 1, %s, 'test', 'test')")
        .formatted(id, UUID.randomUUID(), duration);
  }

  private static String trustmark(final UUID id, final UUID issuerId, final String type) {
    return ("INSERT INTO trustmark (trustmark_id, trustmarkissuer_id, trustmark_type, created_by, last_modified_by, "
        + "last_modified_date) VALUES ('%s', '%s', '%s', 'test', 'test', '%s')")
        .formatted(id, issuerId, type, LAST_MODIFIED);
  }

  private void migrate(final String target) {
    Flyway.configure()
        .dataSource(server.getJdbcUrl(), server.getUsername(), server.getPassword())
        .locations(FlywayLocationsConfiguration.BASELINE, FlywayLocationsConfiguration.MIGRATION)
        .target(target == null ? "latest" : target)
        .load()
        .migrate();
  }

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(server.getJdbcUrl(), server.getUsername(), server.getPassword());
  }
}
