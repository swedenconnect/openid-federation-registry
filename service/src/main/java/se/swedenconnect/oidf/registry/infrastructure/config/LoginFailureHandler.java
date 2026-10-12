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

package se.swedenconnect.oidf.registry.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Sends the browser to the login page of the frontend when the login fails, for example when the tokens can not be
 * exchanged, with an {@code errorcode} that the page turns into a message. The details of the failure are only
 * logged.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class LoginFailureHandler implements AuthenticationFailureHandler {

  /**
   * The login failed.
   */
  public static final String LOGIN_FAILED = "backend.login.failed";

  /**
   * The user, or the identity provider, denied the login.
   */
  public static final String LOGIN_DENIED = "backend.login.denied";

  private static final String ACCESS_DENIED = "access_denied";

  private static final Pattern REASON = Pattern.compile("[a-z][a-z0-9_]{0,63}");

  @Override
  public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
      final AuthenticationException exception) throws IOException {
    log.error("Authentication failed", exception);
    // The context path is added since sendRedirect treats a path starting with a slash as relative to the server.
    final StringBuilder location = new StringBuilder(request.getContextPath())
        .append("/login?errorcode=").append(errorCode(exception));
    reason(exception).ifPresent(reason -> location.append("&reason=").append(reason));
    response.sendRedirect(location.toString());
  }

  /**
   * The OAuth2 error code that Spring Security or the identity provider gave, for example
   * {@code invalid_token_response} when the tokens could not be exchanged. It is only passed on if it is a plain
   * code, so nothing else from the failure reaches the browser.
   *
   * @param exception the failure
   * @return the error code, if there is one
   */
  static Optional<String> reason(final AuthenticationException exception) {
    if (exception instanceof OAuth2AuthenticationException oauth2Exception && oauth2Exception.getError() != null) {
      return Optional.ofNullable(oauth2Exception.getError().getErrorCode())
          .filter(code -> REASON.matcher(code).matches());
    }
    return Optional.empty();
  }

  /**
   * The error code for a failed login.
   *
   * @param exception the failure
   * @return the error code that the login page shows a message for
   */
  static String errorCode(final AuthenticationException exception) {
    if (exception instanceof OAuth2AuthenticationException oauth2Exception
        && oauth2Exception.getError() != null
        && ACCESS_DENIED.equals(oauth2Exception.getError().getErrorCode())) {
      return LOGIN_DENIED;
    }
    return LOGIN_FAILED;
  }
}
