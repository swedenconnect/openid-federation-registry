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
package se.swedenconnect.oidf.registry.infrastructure.persistence;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import se.swedenconnect.oidf.registry.infrastructure.error.ErrorTypes;
import se.swedenconnect.oidf.registry.infrastructure.error.RegistryServerException;

/**
 * Saves a freshly constructed, caller-ID-bearing entity with true insert-only semantics.
 * <p>
 * Every entity with a caller-assignable (non-{@code @GeneratedValue}) primary key in this application implements
 * {@link org.springframework.data.domain.Persistable} so a fresh instance is always treated as new — meaning
 * {@code repository.save(...)} always attempts an INSERT for it, never a merge/UPDATE of an existing row with the same
 * ID. This helper flushes that insert immediately (inside the caller's transaction) and translates the resulting
 * {@link DataIntegrityViolationException} — an existing row with that primary key — into {@link ErrorTypes#CONFLICT},
 * instead of letting a duplicate ID silently overwrite someone else's row or surface as a generic 400.
 *
 * @author Per Fredrik Plars
 */
public final class InsertOnly {

  private InsertOnly() {
  }

  /**
   * Saves {@code entity} with insert-only semantics.
   *
   * @param <T> the entity type
   * @param <ID> the entity's ID type
   * @param repository the repository to save through
   * @param entity a freshly constructed (never-before-persisted) entity
   * @param conflictMessage the message for the {@link ErrorTypes#CONFLICT} exception if the ID already exists
   * @return the saved entity
   * @throws RegistryServerException with {@link ErrorTypes#CONFLICT} if the entity's ID already exists
   */
  public static <T, ID> T save(final JpaRepository<T, ID> repository, final T entity, final String conflictMessage) {
    try {
      return repository.saveAndFlush(entity);
    }
    catch (final DataIntegrityViolationException e) {
      if (isPrimaryKeyViolation(e)) {
        throw new RegistryServerException(ErrorTypes.CONFLICT, conflictMessage, e);
      }
      throw e;
    }
  }

  /**
   * Distinguishes a duplicate-primary-key violation (the caller-selected ID already exists) from any other constraint
   * violation (e.g. a business-level uniqueness rule), which should keep its existing handling rather than being
   * reported as a 409 for an unrelated reason.
   */
  private static boolean isPrimaryKeyViolation(final DataIntegrityViolationException e) {
    Throwable cause = e.getCause();
    while (cause != null) {
      if (cause instanceof final ConstraintViolationException cve) {
        return "PRIMARY".equalsIgnoreCase(cve.getConstraintName());
      }
      cause = cause.getCause();
    }
    return false;
  }
}
