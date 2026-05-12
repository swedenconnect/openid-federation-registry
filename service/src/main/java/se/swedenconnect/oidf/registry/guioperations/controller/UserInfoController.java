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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.guioperations.dto.UserInfoResponse;
import se.swedenconnect.oidf.registry.infrastructure.auth.AuthConstants;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformationFactory;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationInformation;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;

import java.util.Optional;

/**
 * The {@code UserController} class handles the REST API endpoints for retrieving OpenID Connect (OIDC) user orgInfo for
 * the currently authenticated user.
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
   * @param request for getting session values
   * @return a {@code ResponseEntity} containing a {@code UserResponse} object with the user's OpenID Connect orgInfo,
   *     or a 401 Unauthorized status if the user is not authenticated.
   */
  @GetMapping
  public ResponseEntity<UserInfoResponse> getCurrentUser(final HttpServletRequest request) {
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof final OidcUser oidcUser)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return this.handleOrganizationRealm(oidcUser, request);
  }

  /**
   * Selects an org by orgNumber if the orgNumber is present in claims.
   *
   * @param orgNumber to select
   * @param authentication of the request
   * @param request for accessing session values
   */
  @PutMapping
  public void selectOrganization(
      @RequestParam("orgNumber") final String orgNumber,
      final Authentication authentication,
      final HttpServletRequest request) {

    if (authentication.getPrincipal() instanceof OidcUser user) {
      final OrganizationInformation information = OrganizationInformationFactory.getInformation(user);

      final boolean selectedOrgNumberExists =
          information.organizations().stream()
              .anyMatch(org -> org.orgNumber().equals(orgNumber));

      if (selectedOrgNumberExists) {
        request.getSession().setAttribute(AuthConstants.SELECTED_ORG_NUMBER_HEADER_ATTRIBUTE, orgNumber);
      }
    }
  }

  /**
   * Handles the processing of OIDC user information for the "Organization" realm.
   *
   * @param oidcUser the OpenID Connect (OIDC) user providing user-related claims and details
   * @param request the HTTP request, used to retrieve additional session attributes if needed
   * @return a {@code ResponseEntity} containing a {@code OidfUserInfoResponse}, which includes user related information
   */
  private ResponseEntity<UserInfoResponse> handleOrganizationRealm(
      final OidcUser oidcUser, final HttpServletRequest request) {
    final OrganizationInformation orgInfo = OrganizationInformationFactory.getInformation(oidcUser);

    final String attribute = (String) request.getSession().getAttribute(AuthConstants.SELECTED_ORG_NUMBER_HEADER_ATTRIBUTE);
    final OrganizationRecord organizationRecord = Optional.ofNullable(attribute)
        .flatMap(orgNumber -> orgInfo.organizations().stream()
            .filter(org -> org.orgNumber().equals(orgNumber))
            .findFirst())
        .orElseGet(() -> orgInfo.organizations().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No organization available")));

    final UserInfoResponse oidfUserResponse = new UserInfoResponse(
        oidcUser.getPreferredUsername(),
        oidcUser.getGivenName(),
        oidcUser.getFamilyName(),
        oidcUser.getFullName(),
        organizationRecord.orgNumber(),
        organizationRecord.orgName(),
        organizationRecord.entityPrefix(),
        orgInfo
    );

    return ResponseEntity.ok(oidfUserResponse);
  }

}
