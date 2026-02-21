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
package se.swedenconnect.oidf.registry.infrastructure.auth;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import se.swedenconnect.oidf.registry.infrastructure.config.SecurityConfig;

import java.util.Objects;
import java.util.Optional;

/**
 * Implements argument resolver for picking org based on header, if present, or else first.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class OrganizationRecordClaimSelector implements HandlerMethodArgumentResolver {
  public final static String SELECTED_ORG_NUMBER_HEADER_NAME = "selected-org-number";

  /**
   * Determines whether this resolver supports the given method parameter.
   *
   * @param parameter the method parameter to check
   * @return true if the parameter type is OrganizationRecord and the authentication is RegistryClaims
   */
  @Override
  public boolean supportsParameter(final MethodParameter parameter) {
    return parameter.getParameterType().equals(OrganizationRecord.class) &&
        SecurityContextHolder.getContext().getAuthentication() instanceof
            SecurityConfig.RegistryClaims;
  }

  /**
   * Resolves the OrganizationRecord argument from the request header or JWT claims.
   *
   * @param parameter the method parameter
   * @param mavContainer the model and view container
   * @param webRequest the web request
   * @param binderFactory the binder factory
   * @return the OrganizationRecord resolved from the header or JWT claims
   * @throws IllegalArgumentException if the header is missing or the organization is not found in claims
   */
  @Override
  public Object resolveArgument(
      @Nonnull final MethodParameter parameter,
      final ModelAndViewContainer mavContainer,
      final NativeWebRequest webRequest,
      final WebDataBinderFactory binderFactory) {

    final HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    final String selectedOrgNumber = Objects.requireNonNull(request).getHeader(SELECTED_ORG_NUMBER_HEADER_NAME);
    final Object authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof SecurityConfig.RegistryClaims registryClaims) {
      return Optional.ofNullable(selectedOrgNumber)
          .map(registryClaims::getOrganizationRecordByOrgNumber)
          .orElseThrow(
              () -> new IllegalArgumentException("Header:  " + SELECTED_ORG_NUMBER_HEADER_NAME +
                  " missing or have a value that does not match claim in jwt"));
    }
    else {
      throw new IllegalArgumentException("Wrong authentication class, check supportsParameter method.");
    }
  }
}
