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
package se.swedenconnect.oidf.entity.registry.trustmark;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.entity.TrustMarkSubjectEntity;
import se.swedenconnect.oidf.entity.registry.repository.TrustMarkSubjectRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for the {@link TrustMarkSubjectRepository} to ensure proper functionality of CRUD operations on
 * {@link TrustMarkSubjectEntity} entities.
 *
 * @author David Goldring
 */
@DataJpaTest
@ActiveProfiles("h2")
public class TrustMarkSubjectRepositoryTest {

  @Autowired
  private TrustMarkSubjectRepository trustMarkSubjectRepository;

  /**
   * Tests the functionality of saving an {@link TrustMarkSubjectEntity} entity using the
   * {@link TrustMarkSubjectRepository}. This method ensures that the entity is properly persisted to the database and
   * can be retrieved with a valid ID, subject, and entity content.
   */
  @Test
  public void testSaveEntity() {
    // Given
    final TrustMarkSubjectEntity tmi = TrustMarkSubjectEntity.builder()
        .issuer("http://iss")
        .subject("http://sub")
        .trustmarkId("http://tmid")
        .trustmarksubjectJson("{\"name\": \"Example Entity\"}")
        .externalId(UUID.randomUUID().toString())
        .build();

    // When
    TrustMarkSubjectEntity savedtmi = trustMarkSubjectRepository.save(tmi);

    // Then
    assertThat(savedtmi.getSubject()).isEqualTo("http://sub");
    assertThat(savedtmi.getTrustmarksubjectJson()).isEqualTo("{\"name\": \"Example Entity\"}");
  }

  @Test
  public void testSaveEntityDuplicate() {
    // Given
    final TrustMarkSubjectEntity tmi = TrustMarkSubjectEntity.builder()
        .issuer("http://iss")
        .subject("http://sub")
        .trustmarkId("http://tmid")
        .trustmarksubjectJson("{}")
        .externalId(UUID.randomUUID().toString())
        .build();

    // When
    final TrustMarkSubjectEntity savedtmi = this.trustMarkSubjectRepository.save(tmi);

    final TrustMarkSubjectEntity tmiDuplicate = tmi.toBuilder().id(0).build();

    assertThatThrownBy(() -> this.trustMarkSubjectRepository.saveAndFlush(tmiDuplicate)).isInstanceOf(
            DataIntegrityViolationException.class)
        .hasMessageStartingWith("could not execute statement [Unique index or primary key violation");
  }

  /**
   * Tests the functionality of finding an {@link TrustMarkSubjectEntity} entity by its ID using the
   * {@link TrustMarkSubjectRepository}. This method ensures that an entity can be retrieved correctly after being
   * persisted to the database. The test verifies that the ID, subject, and entity content match the values of the saved
   * tmi.
   */
  @Test
  public void testFindById() {
    // Given
    final TrustMarkSubjectEntity tmi = TrustMarkSubjectEntity.builder()
        .issuer("http://iss")
        .subject("http://sub")
        .trustmarkId("http://tmid")
        .trustmarksubjectJson("{}")
        .externalId(UUID.randomUUID().toString())
        .build();

    TrustMarkSubjectEntity savedtmi = this.trustMarkSubjectRepository.save(tmi);

    // When
    Optional<TrustMarkSubjectEntity> foundtmi = this.trustMarkSubjectRepository.findById(savedtmi.getId());

    // Then
    assertThat(foundtmi).isPresent();
    assertThat(foundtmi.get().getId()).isEqualTo(savedtmi.getId());
    assertThat(foundtmi.get().getSubject()).isEqualTo(savedtmi.getSubject());
    assertThat(foundtmi.get().getTrustmarksubjectJson()).isEqualTo(savedtmi.getTrustmarksubjectJson());
  }

  /**
   * Tests the functionality of deleting an {@link TrustMarkSubjectEntity} entity using the
   * {@link TrustMarkSubjectRepository}. This method ensures that the entity is properly removed from the database and
   * cannot be retrieved by its ID after deletion.
   */
  @Test
  public void testDeleteEntity() {
    // Given
    final TrustMarkSubjectEntity tmi = TrustMarkSubjectEntity.builder()
        .issuer("http://iss")
        .subject("http://sub")
        .trustmarkId("http://tmid")
        .trustmarksubjectJson("{}")
        .externalId(UUID.randomUUID().toString())
        .build();

    TrustMarkSubjectEntity savedtmi = this.trustMarkSubjectRepository.save(tmi);

    // When
    this.trustMarkSubjectRepository.deleteById(savedtmi.getId());
    Optional<TrustMarkSubjectEntity> deletedtmi = this.trustMarkSubjectRepository.findById(savedtmi.getId());

    // Then
    assertThat(deletedtmi).isNotPresent();
  }
}