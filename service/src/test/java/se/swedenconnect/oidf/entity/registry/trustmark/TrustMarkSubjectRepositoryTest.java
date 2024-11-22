/*
 * Copyright 2024 Sweden Connect.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.oidf.entity.registry.trustmark;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test class for the {@link TrustMarkSubjectRepository} to ensure proper functionality
 * of CRUD operations on {@link TrustMarkSubjectEntity} entities.
 *
 * @author David Goldring
 */
@DataJpaTest
@ActiveProfiles("h2")
public class TrustMarkSubjectRepositoryTest {

  @Autowired
  private TrustMarkSubjectRepository trustMarkSubjectRepository;

  /**
   * Tests the functionality of saving an {@link TrustMarkSubjectEntity} entity using the {@link TrustMarkSubjectRepository}.
   * This method ensures that the entity is properly persisted to the database and can be retrieved
   * with a valid ID, subject, and entity content.
   */
  @Test
  public void testSaveEntity() {
    // Given
    TrustMarkSubjectEntity tmi = new TrustMarkSubjectEntity();
    tmi.setSubject("https://example.com/subject/1");
    tmi.setTrustmarksubject("{\"name\": \"Example Entity\"}");
    tmi.setIssuer("http://iss");
    tmi.setTrustmarkId("http://tmid.example.se");

    // When
    TrustMarkSubjectEntity savedtmi = trustMarkSubjectRepository.save(tmi);

    // Then
    assertThat(savedtmi.getSubject()).isEqualTo("https://example.com/subject/1");
    assertThat(savedtmi.getTrustmarksubject()).isEqualTo("{\"name\": \"Example Entity\"}");
  }


  @Test
  public void testSaveEntityDuplicate() {
    // Given
    final TrustMarkSubjectEntity tmi = new TrustMarkSubjectEntity();
    tmi.setSubject("https://example.com/subject/1");
    tmi.setTrustmarksubject("{\"name\": \"Example Entity\"}");
    tmi.setIssuer("http://iss");
    tmi.setTrustmarkId("http://tmid.example.se");

    // When
    final TrustMarkSubjectEntity savedtmi = this.trustMarkSubjectRepository.save(tmi);


    final TrustMarkSubjectEntity tmiDuplicate = new TrustMarkSubjectEntity();
    tmiDuplicate.setSubject("https://example.com/subject/1");
    tmiDuplicate.setTrustmarksubject("{\"name\": \"Example Entity\"}");
    tmiDuplicate.setIssuer("http://iss");
    tmiDuplicate.setTrustmarkId("http://tmid.example.se");
    assertThatThrownBy(() -> this.trustMarkSubjectRepository.saveAndFlush(tmiDuplicate)).isInstanceOf(
        DataIntegrityViolationException.class).hasMessageStartingWith("could not execute statement [Unique index or primary key violation");
  }

  /**
   * Tests the functionality of finding an {@link TrustMarkSubjectEntity} entity by its ID using the {@link TrustMarkSubjectRepository}.
   * This method ensures that an entity can be retrieved correctly after being persisted to the database.
   * The test verifies that the ID, subject, and entity content match the values of the saved tmi.
   */
  @Test
  public void testFindById() {
    // Given
    TrustMarkSubjectEntity tmi = new TrustMarkSubjectEntity();
    tmi.setSubject("https://example.com/subject/2");
    tmi.setTrustmarksubject("{\"name\": \"Another Entity\"}");
    tmi.setIssuer("http://iss");
    tmi.setTrustmarkId("http://tmid.example.se");

    TrustMarkSubjectEntity savedtmi = this.trustMarkSubjectRepository.save(tmi);

    // When
    Optional<TrustMarkSubjectEntity> foundtmi = this.trustMarkSubjectRepository.findById(savedtmi.getId());

    // Then
    assertThat(foundtmi).isPresent();
    assertThat(foundtmi.get().getId()).isEqualTo(savedtmi.getId());
    assertThat(foundtmi.get().getSubject()).isEqualTo(savedtmi.getSubject());
    assertThat(foundtmi.get().getTrustmarksubject()).isEqualTo(savedtmi.getTrustmarksubject());
  }

  /**
   * Tests the functionality of deleting an {@link TrustMarkSubjectEntity} entity using the {@link TrustMarkSubjectRepository}.
   * This method ensures that the entity is properly removed from the database and cannot be retrieved
   * by its ID after deletion.
   */
  @Test
  public void testDeleteEntity() {
    // Given
    TrustMarkSubjectEntity tmi = new TrustMarkSubjectEntity();
    tmi.setSubject("https://example.com/subject/3");
    tmi.setTrustmarksubject("{\"name\": \"Entity to be deleted\"}");
    tmi.setIssuer("http://iss");
    tmi.setTrustmarkId("http://tmid.example.se");

    TrustMarkSubjectEntity savedtmi = this.trustMarkSubjectRepository.save(tmi);

    // When
    this.trustMarkSubjectRepository.deleteById(savedtmi.getId());
    Optional<TrustMarkSubjectEntity> deletedtmi = this.trustMarkSubjectRepository.findById(savedtmi.getId());

    // Then
    assertThat(deletedtmi).isNotPresent();
  }
}