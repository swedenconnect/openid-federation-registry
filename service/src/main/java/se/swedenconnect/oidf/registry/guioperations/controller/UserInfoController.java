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
package se.swedenconnect.oidf.registry.guioperations.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.guioperations.dto.UserInfoResponse;

/**
 * The {@code UserController} class handles the REST API endpoints for retrieving OpenID Connect (OIDC) user orgInfo for
 * the currently authenticated user. Organization/tenant selection, and the per-organization entityPrefix, are
 * resolved purely from the {@code /tenants} endpoint and kept client-side; this controller only reports the
 * authenticated user's own identity claims.
 *
 * @author David Goldring
 */
@Slf4j
@RestController
@Hidden
@RequestMapping("/userinfo")
public class UserInfoController {

  /**
   * Retrieves the orgInfo of the currently authenticated OpenID Connect (OIDC) user. If the user is not authenticated
   * or the authentication orgInfo is invalid, the method returns an unauthorized response.
   *
   * @return a {@code ResponseEntity} containing a {@code UserResponse} object with the user's OpenID Connect orgInfo,
   *     or a 401 Unauthorized status if the user is not authenticated.
   */
  @GetMapping
  public ResponseEntity<UserInfoResponse> getCurrentUser() {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof final OidcUser oidcUser)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(new UserInfoResponse(
        oidcUser.getPreferredUsername(),
        oidcUser.getGivenName(),
        oidcUser.getFamilyName(),
        oidcUser.getFullName()
    ));
  }

}
