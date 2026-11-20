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
package se.swedenconnect.oidf.registry.federationservice.state;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.test.util.ReflectionTestUtils;
import se.swedenconnect.oidf.registry.infrastructure.audit.FederationAuditEvent;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for the {@link StateChangeRecorder} class.
 *
 * @author Felix Hellman
 */
class StateChangeRecorderTest {

  @BeforeEach
  void initSynchronization() {
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void clearSynchronization() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  @DisplayName("A write conflict raised by the flush before commit is translated to a DataAccessException")
  void writeConflictInFlushIsTranslated() {
    final StateDependencies dependencies = mock(StateDependencies.class);
    when(dependencies.affectsRecords(any())).thenReturn(true);
    final EntityManager entityManager = mock(EntityManager.class);
    final SQLException conflict = new SQLException("Record has changed since last read in table 'entities'",
        "HY000", 1020);
    doThrow(new GenericJDBCException("could not execute statement", conflict)).when(entityManager).flush();

    final StateChangeRecorder recorder = new StateChangeRecorder(dependencies, mock(StateChangeStore.class),
        mock(PlatformTransactionManager.class));
    ReflectionTestUtils.setField(recorder, "entityManager", entityManager);

    recorder.onAuditEvent(FederationAuditEvent.builder().build());
    final TransactionSynchronization pending = TransactionSynchronizationManager.getSynchronizations().getFirst();

    assertThatThrownBy(() -> pending.beforeCommit(false))
        .isInstanceOf(DataAccessException.class)
        .satisfies(e -> assertThat(((DataAccessException) e).getMostSpecificCause()).isSameAs(conflict));
  }
}
