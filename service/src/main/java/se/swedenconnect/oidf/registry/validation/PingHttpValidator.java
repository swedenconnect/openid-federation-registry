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
package se.swedenconnect.oidf.registry.validation;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Doing an outgoing call to verify that the url exist and return 200 ok to the GET request
 *
 * @author Per Fredrik Plars
 */
public class PingHttpValidator implements PropertyValidatorPlugin {

  private static final int MAX_REQUESTS_PER_MINUTE = 120;
  private static final Deque<Long> requestTimestamps = new ArrayDeque<>();

  @Override
  public ValidationStatus validate(final String key, final String value) {
    if (value == null || value.isBlank()) {
      return ValidationStatusImpl.noEval(key, this.name());
    }

    final URL url;
    try {
      url = URI.create(value).toURL();
    }
    catch (final MalformedURLException e) {
      throw new PropertyValidationFailException(key, value, e.getMessage());
    }

    this.rateLimit();

    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) url.openConnection();
      connection.setInstanceFollowRedirects(false); // följ inte 302
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(1000);
      connection.setReadTimeout(1000);
      connection.connect();

      final int status = connection.getResponseCode();
      if (status != 200) {
        throw new PropertyValidationFailException(key, "", "Expected 200 OK but got " + status);
      }

      try (InputStream in = connection.getInputStream()) {
        final byte[] buffer = new byte[1024]; // max 1 KB
        final int read = in.read(buffer);
        if (read == -1) {
          throw new PropertyValidationFailException(key, "", "Empty response body");
        }
      }
      catch (final ProtocolException e) {
        throw new PropertyValidationFailException(key, "", "Unsupported protokoll:" + e.getMessage());

      }
    }
    catch (final IOException e) {
      throw new PropertyValidationFailException(key, "", "Unable to call url:" + e.getMessage());
    }

    finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    return ValidationStatusImpl.ok(key, this.name());
  }

  private synchronized void rateLimit() throws PropertyValidationFailException {
    final long now = Instant.now().toEpochMilli();
    final long oneMinuteAgo = now - 60_000;

    while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() < oneMinuteAgo) {
      requestTimestamps.removeFirst();
    }

    if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
      throw new IllegalArgumentException("Rate limit exceeded: max "
          + MAX_REQUESTS_PER_MINUTE + " requests per minute");
    }
    requestTimestamps.addLast(now);
  }

  @Override
  public String name() {
    return "ping";
  }
}
