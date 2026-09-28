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
package se.swedenconnect.oidf.registry.organization.service;

import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

/**
 * Matches the host of an entity identifier against an organization's registered domains.
 *
 * <p>A host matches a domain when it is that domain, or a subdomain of it: {@code sp.example.com} matches
 * {@code example.com} but {@code notexample.com} does not. Pure and side-effect free, because both the
 * enforcement of registration requests and the cascade of a domain rejection depend on exactly the same rule.
 *
 * @author Felix Hellman
 */
public final class DomainMatcher {

  private DomainMatcher() {
  }

  /**
   * Extracts the lower-cased host of an entity identifier.
   *
   * @param entityIdentifier the entity identifier, expected to be an absolute URL
   * @return the host, or empty if the identifier is null, unparseable or carries no host
   */
  public static Optional<String> hostOf(final String entityIdentifier) {
    if (entityIdentifier == null || entityIdentifier.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(URI.create(entityIdentifier.trim()).getHost())
          .map(host -> host.toLowerCase(Locale.ROOT));
    }
    catch (final IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  /**
   * Tests whether a host equals, or is a subdomain of, the given domain.
   *
   * @param host the host to test, compared case-insensitively
   * @param domain the registered domain
   * @return true if the host is covered by the domain
   */
  public static boolean matches(final String host, final String domain) {
    if (host == null || domain == null || host.isBlank() || domain.isBlank()) {
      return false;
    }
    final String normalizedHost = host.toLowerCase(Locale.ROOT);
    final String normalizedDomain = domain.toLowerCase(Locale.ROOT);
    return normalizedHost.equals(normalizedDomain) || normalizedHost.endsWith("." + normalizedDomain);
  }

  /**
   * Tests whether a host is covered by any of the given domains.
   *
   * @param host the host to test
   * @param domains the registered domains
   * @return true if at least one domain covers the host
   */
  public static boolean matchesAny(final String host, final Collection<String> domains) {
    if (domains == null) {
      return false;
    }
    return domains.stream().anyMatch(domain -> matches(host, domain));
  }
}
