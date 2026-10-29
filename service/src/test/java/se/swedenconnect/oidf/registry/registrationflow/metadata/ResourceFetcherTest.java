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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.model.DomainStatus;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.model.OrganizationDomain;
import se.swedenconnect.oidf.registry.organization.repository.OrganizationDomainRepository;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceFetcherTest {

  private static final OrganizationRecord ORG = new OrganizationRecord("2021006883", "Digg", null, "Swedenconnect");

  private HttpClient httpClient;
  private ResourceFetcher fetcher;

  @BeforeEach
  void setUp() {
    final Organization organization = new Organization();
    organization.setOrganizationId(UUID.randomUUID());
    final OrganizationDomain validated = new OrganizationDomain();
    validated.setDomain("example.se");
    validated.setStatus(DomainStatus.VALIDATED);

    final OrganizationService organizationService = mock(OrganizationService.class);
    when(organizationService.find(ORG)).thenReturn(Optional.of(organization));
    final OrganizationDomainRepository domains = mock(OrganizationDomainRepository.class);
    when(domains.findByOrganization_OrganizationIdAndStatusIn(organization.getOrganizationId(),
        List.of(DomainStatus.VALIDATED))).thenReturn(List.of(validated));

    this.httpClient = mock(HttpClient.class);
    this.fetcher = new ResourceFetcher(organizationService, domains, this.httpClient);
  }

  @SuppressWarnings("unchecked")
  private void serve(final int status, final String contentType, final byte[] body) throws Exception {
    final HttpResponse<InputStream> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.headers()).thenReturn(HttpHeaders.of(
        contentType == null ? Map.of() : Map.of("Content-Type", List.of(contentType)), (name, value) -> true));
    when(response.body()).thenReturn(new ByteArrayInputStream(body));
    when(this.httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) response);
  }

  @Test
  @DisplayName("An image on a validated domain is fetched and kept as a data URI")
  void fetchesImageOnValidatedDomain() throws Exception {
    this.serve(200, "image/svg+xml; charset=utf-8", "<svg/>".getBytes());

    final FetchedResource image = this.fetcher.fetchImage("https://www.example.se/logo.svg", ORG, null);

    assertThat(image.isImage()).isTrue();
    assertThat(image.contentType()).isEqualTo("image/svg+xml");
    assertThat(image.size()).isEqualTo(6);
    assertThat(image.dataUri()).isEqualTo("data:image/svg+xml;base64,PHN2Zy8+");
  }

  @Test
  @DisplayName("An image under a self-hosted entity's own host is fetched without a validated domain")
  void fetchesImageUnderSelfHostedEntityHost() throws Exception {
    this.serve(200, "image/png", new byte[] { 1, 2, 3 });

    final FetchedResource image = this.fetcher.fetchImage("https://cdn.rp.other.se/logo.png", ORG,
        "https://rp.other.se/oidc");

    assertThat(image.isImage()).isTrue();
    assertThat(image.error()).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("A self-hosted entity's host does not cover other hosts")
  void refusesHostOutsideSelfHostedEntityHost() throws Exception {
    assertThat(this.fetcher.fetchImage("https://other.se/logo.png", ORG, "https://rp.other.se").error())
        .contains("neither under the entity's own host");
    assertThat(this.fetcher.fetchImage("https://rp.other.se.evil.com/logo.png", ORG, "https://rp.other.se").error())
        .contains("neither under the entity's own host");
    verify(this.httpClient, never()).send(any(), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("Plain HTTP and hosts outside the validated domains are not fetched at all")
  void refusesUntrustedUrls() throws Exception {
    assertThat(this.fetcher.fetchImage("http://www.example.se/logo.svg", ORG, null).error())
        .contains("only HTTPS");
    assertThat(this.fetcher.fetchImage("https://cdn.elsewhere.com/logo.svg", ORG, null).error())
        .contains("not under a validated domain");
    assertThat(this.fetcher.fetchImage("https://example.se.evil.com/logo.svg", ORG, null).error())
        .contains("not under a validated domain");
    verify(this.httpClient, never()).send(any(), any(HttpResponse.BodyHandler.class));
  }

  @Test
  @DisplayName("Something that is not an image, too large, or an error answer is reported, not kept")
  void reportsUnusableResponses() throws Exception {
    this.serve(200, "text/html", "<html/>".getBytes());
    assertThat(this.fetcher.fetchImage("https://example.se/logo", ORG, null))
        .satisfies(r -> assertThat(r.isImage()).isFalse())
        .satisfies(r -> assertThat(r.error()).contains("text/html"))
        .satisfies(r -> assertThat(r.dataUri()).isNull());

    this.serve(200, "image/png", new byte[ResourceFetcher.MAX_BYTES + 1]);
    assertThat(this.fetcher.fetchImage("https://example.se/big.png", ORG, null).error()).contains("Larger than");

    this.serve(404, "text/plain", new byte[0]);
    assertThat(this.fetcher.fetchImage("https://example.se/gone.png", ORG, null).error()).contains("HTTP 404");
  }
}
