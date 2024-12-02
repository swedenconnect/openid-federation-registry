/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.trustmark;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

/**
 * TrustMarkController is a REST controller for managing policies in the entity registry. It provides endpoints for
 * creating, updating, retrieving, and deleting trustMark records.
 * <p>
 * The controller utilizes {@link TrustMarkSubjectService} to perform the required operations.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@RestController
@RequestMapping("/registry/v1/trustmarksubject")
public class TrustMarkSubjectController {

  /**
   * The {@code TrustMarkSubjectService} is an instance of the {@link TrustMarkSubjectService} class.
   */
  private final TrustMarkSubjectService trustMarkSubjectService;

  /**
   * Constructs a new TrustMarkController with the specified TrustMarkSubjectService implementation.
   *
   * @param trustMarkSubjectService the {@link TrustMarkSubjectService} implementation used for managing trustMark
   *     operations
   */
  public TrustMarkSubjectController(
      @Qualifier("jpaTrustMarkSubjectService")final TrustMarkSubjectService trustMarkSubjectService) {
    this.trustMarkSubjectService = trustMarkSubjectService;
  }

  /**
   * Creates a new trustMark in the entity registry.
   *
   * @param trustMark a {@link TrustMarkSubjectRecord} object containing the details of the trustMark to be created
   * @param response the {@link HttpServletResponse} object used to set the response status
   * @return a {@link TrustMarkSubjectRecord} object representing the created trustMark
   */
  @PostMapping
  public TrustMarkSubjectRecord createTrustMarkSubject(@RequestBody final TrustMarkSubjectRecord trustMark,
      final HttpServletResponse response) {
    log.debug("POST: {}", trustMark);

    final TrustMarkSubjectRecord record = this.trustMarkSubjectService.create(trustMark);
    response.setStatus(HttpServletResponse.SC_CREATED);
    return record;
  }

  /**
   * Retrieves a trustMarks by its issuer
   *
   * @param trustMarkSubjectId the name of the trustMark to be retrieved
   * @return a {@link TrustMarkSubjectRecord} object representing the trustMark, if found
   */
  @GetMapping("/{trustMarkSubjectId}")
  public TrustMarkSubjectRecord getTrustMarkSubjectById(
      @PathVariable("trustMarkSubjectId") final String trustMarkSubjectId) {
    log.debug("GET by trustMarkSubjectId: {}", trustMarkSubjectId);

    final TrustMarkSubjectRecord record = this.trustMarkSubjectService.get(trustMarkSubjectId);
    if (record == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrustMark not found");
    }
    return record;
  }

  /**
   * Updates an existing trustMark in the entity registry.
   *
   * @param trustMarkSubjectId The trustMarkSubjectId of the trustMarkSubject to update.
   * @param trustMark A {@link TrustMarkSubjectRecord} object containing the updated details of the
   *     trustMarkSubject.
   * @return A {@link TrustMarkSubjectRecord} object representing the updated trustMark.
   */
  @PutMapping("/{trustMarkSubjectId}")
  public TrustMarkSubjectRecord updateTrustMarkSubject(
      @PathVariable("trustMarkSubjectId") final String trustMarkSubjectId,
      @RequestBody final TrustMarkSubjectRecord trustMark) {
    log.debug("PUT: {}", trustMark);
    if (!trustMarkSubjectId.equals(trustMark.getTrustMarkSubjectRecordId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TrustMarkId has to be the same in path and object");
    }
    return this.trustMarkSubjectService.update(trustMarkSubjectId, trustMark);
  }

  /**
   * Deletes a trustMark by its trustMarkSubjectId from the entity registry.
   *
   * @param trustMarkSubjectId the trustMarkSubjectId of the trustMarkSubject to be deleted
   * @param response the {@link HttpServletResponse} object used to set the response status
   */
  @DeleteMapping("/{trustMark_id}")
  public void deleteTrustMark(@PathVariable("trustMarkSubjectId") final String trustMarkSubjectId,
      final HttpServletResponse response) {
    log.debug("DELETE: {}", trustMarkSubjectId);

    this.trustMarkSubjectService.delete(trustMarkSubjectId);
    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
