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
package se.swedenconnect.oidf.registry.registrationflow.metadata;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationDomainRepository;
import se.swedenconnect.oidf.registry.organization.service.DomainMatcher;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Fetches images an entity's metadata points to, such as its logo, so the reviewer sees them and checks can tell an
 * image from anything else. Also fetches JSON documents, such as the redirect URIs a {@code sector_identifier_uri}
 * lists.
 *
 * <p>The registry only fetches from hosts it has reason to trust: an HTTPS URL whose host is under a domain the
 * tenant operator has validated for the entity's organization. Redirects are not followed, so a validated host cannot
 * point the registry elsewhere. Anything else is reported as not fetched, with the reason.</p>
 *
 * @author Felix Hellman
 */
@Slf4j
@Component
public class ResourceFetcher {

  /** The largest image the registry fetches and keeps. */
  static final int MAX_BYTES = 512 * 1024;

  /** The largest JSON document the registry fetches and keeps. */
  static final int MAX_JSON_BYTES = 64 * 1024;

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private final OrganizationService organizationService;
  private final OrganizationDomainRepository organizationDomainRepository;
  private final HttpClient httpClient;

  /**
   * Constructor.
   *
   * @param organizationService service resolving the organization behind the entity
   * @param organizationDomainRepository repository of the domains organizations have claimed
   */
  @Autowired
  public ResourceFetcher(final OrganizationService organizationService,
      final OrganizationDomainRepository organizationDomainRepository) {
    this(organizationService, organizationDomainRepository, HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build());
  }

  ResourceFetcher(final OrganizationService organizationService,
      final OrganizationDomainRepository organizationDomainRepository, final HttpClient httpClient) {
    this.organizationService = organizationService;
    this.organizationDomainRepository = organizationDomainRepository;
    this.httpClient = httpClient;
  }

  /**
   * Fetches an image if the URL is one the registry fetches from.
   *
   * @param url the image URL
   * @param organization the organization behind the entity
   * @return the outcome; never throws
   */
  public FetchedResource fetchImage(final String url, final OrganizationRecord organization) {
    final URI uri;
    try {
      uri = URI.create(url.trim());
    }
    catch (final IllegalArgumentException e) {
      return FetchedResource.notFetched(url, "Not a valid URL");
    }
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      return FetchedResource.notFetched(url, "Not fetched: only HTTPS URLs are fetched");
    }
    final Optional<String> host = DomainMatcher.hostOf(url);
    if (host.isEmpty() || !DomainMatcher.matchesAny(host.get(), this.validatedDomains(organization))) {
      return FetchedResource.notFetched(url,
          "Not fetched: the host is not under a validated domain of the organization");
    }
    try {
      final HttpResponse<InputStream> response = this.httpClient.send(HttpRequest.newBuilder(uri)
          .timeout(TIMEOUT)
          .header("Accept", "image/*")
          .GET()
          .build(), HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream body = response.body()) {
        if (response.statusCode() != 200) {
          return FetchedResource.notFetched(url, "The server answered HTTP " + response.statusCode());
        }
        final String contentType = response.headers().firstValue("Content-Type")
            .map(value -> value.split(";")[0].trim().toLowerCase(Locale.ROOT))
            .orElse(null);
        final byte[] content = body.readNBytes(MAX_BYTES + 1);
        if (content.length > MAX_BYTES) {
          return new FetchedResource(url, contentType, null, null,
              "Larger than %d KB".formatted(MAX_BYTES / 1024));
        }
        if (contentType == null || !contentType.startsWith("image/")) {
          return new FetchedResource(url, contentType, (long) content.length, null,
              "Not an image: the server sent " + (contentType == null ? "no content type" : contentType));
        }
        return new FetchedResource(url, contentType, (long) content.length,
            "data:%s;base64,%s".formatted(contentType, Base64.getEncoder().encodeToString(content)), null);
      }
    }
    catch (final IOException e) {
      log.info("Could not fetch {}: {}", url, e.getMessage());
      return FetchedResource.notFetched(url, "Could not fetch: " + e.getMessage());
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return FetchedResource.notFetched(url, "Fetching was interrupted");
    }
  }

  /**
   * Fetches a JSON document if the URL is one the registry fetches from, under the same rules as an image. The document
   * is kept as an {@code application/json} {@code data:} URI.
   *
   * @param url the document URL
   * @param organization the organization behind the entity
   * @return the outcome; never throws
   */
  public FetchedResource fetchJson(final String url, final OrganizationRecord organization) {
    final URI uri;
    try {
      uri = URI.create(url.trim());
    }
    catch (final IllegalArgumentException e) {
      return FetchedResource.notFetched(url, "Not a valid URL");
    }
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      return FetchedResource.notFetched(url, "Not fetched: only HTTPS URLs are fetched");
    }
    final Optional<String> host = DomainMatcher.hostOf(url);
    if (host.isEmpty() || !DomainMatcher.matchesAny(host.get(), this.validatedDomains(organization))) {
      return FetchedResource.notFetched(url,
          "Not fetched: the host is not under a validated domain of the organization");
    }
    try {
      final HttpResponse<InputStream> response = this.httpClient.send(HttpRequest.newBuilder(uri)
          .timeout(TIMEOUT)
          .header("Accept", "application/json")
          .GET()
          .build(), HttpResponse.BodyHandlers.ofInputStream());
      try (InputStream body = response.body()) {
        if (response.statusCode() != 200) {
          return FetchedResource.notFetched(url, "The server answered HTTP " + response.statusCode());
        }
        final String contentType = response.headers().firstValue("Content-Type")
            .map(value -> value.split(";")[0].trim().toLowerCase(Locale.ROOT))
            .orElse(null);
        final byte[] content = body.readNBytes(MAX_JSON_BYTES + 1);
        if (content.length > MAX_JSON_BYTES) {
          return new FetchedResource(url, contentType, null, null,
              "Larger than %d KB".formatted(MAX_JSON_BYTES / 1024));
        }
        try {
          new JSONParser(JSONParser.MODE_RFC4627).parse(new String(content, StandardCharsets.UTF_8));
        }
        catch (final ParseException e) {
          return new FetchedResource(url, contentType, (long) content.length, null, "Not a JSON document");
        }
        return new FetchedResource(url, contentType, (long) content.length,
            "data:application/json;base64," + Base64.getEncoder().encodeToString(content), null);
      }
    }
    catch (final IOException e) {
      log.info("Could not fetch {}: {}", url, e.getMessage());
      return FetchedResource.notFetched(url, "Could not fetch: " + e.getMessage());
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return FetchedResource.notFetched(url, "Fetching was interrupted");
    }
  }

  private List<String> validatedDomains(final OrganizationRecord organization) {
    return this.organizationService.find(organization)
        .map(org -> this.organizationDomainRepository.findByOrganization_OrganizationIdAndStatusIn(
            org.getOrganizationId(), List.of(DomainStatus.VALIDATED)))
        .orElse(List.of())
        .stream()
        .map(OrganizationDomain::getDomain)
        .toList();
  }
}
