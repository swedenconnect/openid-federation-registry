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
package se.swedenconnect.oidf.registry.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import se.swedenconnect.oidf.registry.config.SecurityConfig;
import se.swedenconnect.oidf.registry.entity.OrganizationEntity;

import java.util.Optional;

/**
 * Implements argument resolver for picking org based on header, if present, or else first.
 *
 * @author Felix Hellman
 */
@Slf4j
public class OrganizationInformationSelector implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(final MethodParameter parameter) {
    return parameter.getParameterType().equals(OrganizationEntity.class) &&
        SecurityContextHolder.getContext().getAuthentication() instanceof
            SecurityConfig.RegistryClaims;
  }

  @Override
  public Object resolveArgument(
      final MethodParameter parameter,
      final ModelAndViewContainer mavContainer,
      final NativeWebRequest webRequest,
      final WebDataBinderFactory binderFactory) throws Exception {

    final HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    final String selectedOrgNumber = request.getHeader("selected-org-number");
    final Object authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof SecurityConfig.RegistryClaims registryClaims) {
      return Optional.ofNullable(selectedOrgNumber)
          .map(registryClaims::getByOrgNumber)
          .orElseGet(() -> registryClaims.getOrg().getFirst());
    }
    else {
      throw new IllegalArgumentException("Wrong authentication class, check supportsParameter method.");
    }
  }
}
