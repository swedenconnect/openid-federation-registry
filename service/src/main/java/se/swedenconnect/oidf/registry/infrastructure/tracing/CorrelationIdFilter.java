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

package se.swedenconnect.oidf.registry.infrastructure.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that establishes a {@link CorrelationId} for every incoming HTTP request.
 * <p>
 * If the request carries a {@value CorrelationId#HEADER_NAME} header whose value is a valid UUID,
 * that value is used. Otherwise a new random UUID is generated. The correlation ID is stored in
 * the SLF4J MDC for the duration of the request and removed afterwards so it does not leak to
 * subsequent requests processed on the same thread.
 *
 * @author Per Fredrik Plars
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
      final FilterChain filterChain) throws ServletException, IOException {
    final String correlationId = resolveCorrelationId(request);
    try {
      CorrelationId.set(correlationId);
      response.setHeader(CorrelationId.HEADER_NAME, correlationId);
      filterChain.doFilter(request, response);
    }
    finally {
      CorrelationId.clear();
    }
  }

  private static String resolveCorrelationId(final HttpServletRequest request) {
    final String header = request.getHeader(CorrelationId.HEADER_NAME);
    if (header != null && !header.isBlank()) {
      try {
        return UUID.fromString(header.trim()).toString();
      }
      catch (final IllegalArgumentException ignored) {
      }
    }
    return UUID.randomUUID().toString();
  }
}