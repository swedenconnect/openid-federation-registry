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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

class LoginFailureHandlerTest {

  private final LoginFailureHandler handler = new LoginFailureHandler();

  private String redirectFor(final String contextPath, final org.springframework.security.core.AuthenticationException e)
      throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath(contextPath);
    final MockHttpServletResponse response = new MockHttpServletResponse();
    this.handler.onAuthenticationFailure(request, response, e);
    return response.getRedirectedUrl();
  }

  @Test
  @DisplayName("A failed token exchange goes to the login page with the general error code")
  void tokenExchangeFailure() throws Exception {
    assertThat(this.redirectFor("", new OAuth2AuthenticationException(new OAuth2Error("invalid_token_response"))))
        .isEqualTo("/login?errorcode=backend.login.failed&reason=invalid_token_response");
  }

  @Test
  @DisplayName("A denied login has its own error code")
  void deniedLogin() throws Exception {
    assertThat(this.redirectFor("", new OAuth2AuthenticationException(new OAuth2Error("access_denied"))))
        .isEqualTo("/login?errorcode=backend.login.denied&reason=access_denied");
  }

  @Test
  @DisplayName("Other failures give the general error code")
  void otherFailure() throws Exception {
    assertThat(this.redirectFor("", new BadCredentialsException("bad")))
        .isEqualTo("/login?errorcode=backend.login.failed");
  }

  @Test
  @DisplayName("An error code that is not a plain code is not passed on to the browser")
  void unsafeReasonIsLeftOut() throws Exception {
    assertThat(this.redirectFor("", new OAuth2AuthenticationException(new OAuth2Error("x&y=<script>"))))
        .isEqualTo("/login?errorcode=backend.login.failed");
  }

  @Test
  @DisplayName("The context path is kept in the redirect")
  void contextPath() throws Exception {
    assertThat(this.redirectFor("/oidf-admin", new BadCredentialsException("bad")))
        .isEqualTo("/oidf-admin/login?errorcode=backend.login.failed");
  }
}
