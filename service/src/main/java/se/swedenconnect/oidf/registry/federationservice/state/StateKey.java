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

import java.util.Optional;
import java.util.UUID;

/**
 * The key of one record that the federation services read. A key is its type, a colon and a database ID:
 * {@code entity:<id>}, {@code ta:<id>}, {@code tmi:<id>} and {@code resolver:<id>}. A subordinate of a Trust Anchor
 * or Intermediate is {@code ta:<id>|sub:<id>}.
 *
 * @param type the type of the record
 * @param id the ID of the record, the subordinate ID for a subordinate
 * @param trustAnchorId the ID of the Trust Anchor or Intermediate of a subordinate, {@code null} for other types
 * @author Felix Hellman
 */
public record StateKey(Type type, UUID id, UUID trustAnchorId) {

  /** The operation of a change that updates or adds a record. */
  public static final String UPSERT = "upsert";

  /** The operation of a change that removes a record. */
  public static final String DELETE = "delete";

  private static final String SUB_SEPARATOR = "|sub:";

  /**
   * The type of a record.
   */
  public enum Type {
    /** An element of entity_records. */
    ENTITY("entity"),
    /** A Trust Anchor or Intermediate of module_records, without its subordinates. */
    TRUST_ANCHOR("ta"),
    /** A subordinate of a Trust Anchor or Intermediate. */
    SUBORDINATE("ta"),
    /** A trust mark issuer of module_records. */
    TRUST_MARK_ISSUER("tmi"),
    /** A resolver of module_records. */
    RESOLVER("resolver");

    private final String prefix;

    Type(final String prefix) {
      this.prefix = prefix;
    }
  }

  /**
   * The key of an entity record.
   *
   * @param entityId the entity ID
   * @return the key
   */
  public static String entity(final UUID entityId) {
    return Type.ENTITY.prefix + ":" + entityId;
  }

  /**
   * The key of a Trust Anchor or Intermediate.
   *
   * @param moduleId the module ID
   * @return the key
   */
  public static String trustAnchor(final UUID moduleId) {
    return Type.TRUST_ANCHOR.prefix + ":" + moduleId;
  }

  /**
   * The key of a subordinate of a Trust Anchor or Intermediate.
   *
   * @param moduleId the module ID of the Trust Anchor or Intermediate
   * @param subordinateId the subordinate ID
   * @return the key
   */
  public static String subordinate(final UUID moduleId, final UUID subordinateId) {
    return trustAnchor(moduleId) + SUB_SEPARATOR + subordinateId;
  }

  /**
   * The key of a trust mark issuer.
   *
   * @param moduleId the module ID
   * @return the key
   */
  public static String trustMarkIssuer(final UUID moduleId) {
    return Type.TRUST_MARK_ISSUER.prefix + ":" + moduleId;
  }

  /**
   * The key of a resolver.
   *
   * @param moduleId the module ID
   * @return the key
   */
  public static String resolver(final UUID moduleId) {
    return Type.RESOLVER.prefix + ":" + moduleId;
  }

  /**
   * Parses a key. Only the canonical form, with lower case IDs, is accepted.
   *
   * @param key the key
   * @return the parsed key, or empty if it is not a key
   */
  public static Optional<StateKey> parse(final String key) {
    if (key == null) {
      return Optional.empty();
    }
    final int colon = key.indexOf(':');
    if (colon < 0) {
      return Optional.empty();
    }
    final String prefix = key.substring(0, colon);
    final String rest = key.substring(colon + 1);
    try {
      if (prefix.equals(Type.TRUST_ANCHOR.prefix)) {
        final int separator = rest.indexOf(SUB_SEPARATOR);
        if (separator < 0) {
          return canonical(key, new StateKey(Type.TRUST_ANCHOR, UUID.fromString(rest), null));
        }
        final UUID moduleId = UUID.fromString(rest.substring(0, separator));
        final UUID subordinateId = UUID.fromString(rest.substring(separator + SUB_SEPARATOR.length()));
        return canonical(key, new StateKey(Type.SUBORDINATE, subordinateId, moduleId));
      }
      for (final Type type : new Type[] { Type.ENTITY, Type.TRUST_MARK_ISSUER, Type.RESOLVER }) {
        if (prefix.equals(type.prefix)) {
          return canonical(key, new StateKey(type, UUID.fromString(rest), null));
        }
      }
      return Optional.empty();
    }
    catch (final IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static Optional<StateKey> canonical(final String key, final StateKey parsed) {
    if (parsed.toString().equals(key)) {
      return Optional.of(parsed);
    }
    return Optional.empty();
  }

  /**
   * The key as a string.
   *
   * @return the key
   */
  @Override
  public String toString() {
    return switch (this.type) {
      case ENTITY -> entity(this.id);
      case TRUST_ANCHOR -> trustAnchor(this.id);
      case SUBORDINATE -> subordinate(this.trustAnchorId, this.id);
      case TRUST_MARK_ISSUER -> trustMarkIssuer(this.id);
      case RESOLVER -> resolver(this.id);
    };
  }
}
