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

package se.swedenconnect.oidf.entity.registry.service;

import com.nimbusds.jose.jwk.JWK;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import se.swedenconnect.oidf.entity.registry.audit.FederationAuditEvent;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The NotifyService class is responsible for sending asynchronous notifications to specified endpoints based on events
 * related to federation auditing. It utilizes a pool of virtual threads for submitting and managing notification
 * tasks.
 *
 * This service primarily listens for {@link FederationAuditEvent} events and processes them by invoking a set of
 * pre-configured notification endpoints with structured payload data.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class NotifyService {
  final List<URI> notificationEndPoints;

  final RestClient restClient;
  final JWTSupport jwtSupport;

  final ExecutorService executorService;

  /**
   * Constructs a NotifyService instance to handle sending asynchronous notifications to the specified endpoints using
   * JWT-signed payloads. The service utilizes a virtual thread pool for task execution.
   *
   * @param restClient the {@link RestClient} used to make HTTP requests to the notification endpoints
   * @param notificationEndPoints a list of {@link URI} objects representing the target notification endpoints
   * @param signKey the {@link JWK} signing key used to generate JWT-signed payloads for notifications
   */
  public NotifyService(final RestClient restClient,
      final List<URI> notificationEndPoints,
      final JWK signKey) {

    this.restClient = restClient;

    this.notificationEndPoints = notificationEndPoints;
    this.jwtSupport = new JWTSupport(signKey);
    this.executorService = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Handles audit events by processing federation audit events and notifying configured endpoints.
   *
   * @param event the {@link FederationAuditEvent} containing the audit event details to process
   */
  @EventListener
  public void onAuditEvent(final FederationAuditEvent event) {
    if (event.getFkKeyType() != null) {

      this.notificationEndPoints.forEach(endpoint -> {
        this.executorService.submit(() -> {
          try {
            final String payload = this.createPayLoad(event);
            this.callNotifyEndpoint(endpoint, payload);
          }
          catch (final Exception e) {
            log.warn("Problem calling notify endpoint: {}", endpoint, e);
          }
        });
      });

    }
  }

  protected void callNotifyEndpoint(final URI endpoint, final String payload) {
    log.debug("Calling notify endpoint:{} payload:{}", endpoint, payload);
    final ResponseEntity<String> response = this.restClient
        .post()
        .uri(endpoint)
        .body(payload)
        .retrieve()
        .toEntity(String.class);
    if (response.getStatusCode().isError()) {
      throw new RuntimeException("Problem calling notify endpoint: %s - %s '%s'"
          .formatted(endpoint, response.getStatusCode(), response.getBody()));
    }
  }

  protected String createPayLoad(final FederationAuditEvent event) {
    final Map<String, Object> data = Map.of("action", event.getEvent(),
        "eventtype", event.getFkKeyType(),
        "id", event.getOptionId(),
        "uri", "/api/v1/options/%s/%s".formatted(event.getFkKeyType(), event.getOptionId()));
    return this.jwtSupport.signJWT("notification",
            builder -> builder.claim("notification", data))
        .serialize();
  }

  /**
   * Terminates the ExecutorService associated with the instance. This method attempts to await termination of the
   * ExecutorService for a maximum of one second. If termination is not achieved within the specified duration, the
   * ExecutorService is forcibly shut down.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting for the ExecutorService to
   *     terminate
   */
  @PostConstruct
  public void destroy() throws InterruptedException {
    if (!this.executorService.awaitTermination(1, TimeUnit.SECONDS)) {
      this.executorService.shutdownNow();
    }
  }
}
