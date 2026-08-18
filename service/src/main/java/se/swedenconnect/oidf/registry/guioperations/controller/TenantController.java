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


import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.swedenconnect.oidf.registry.guioperations.TenantService;
import se.swedenconnect.oidf.registry.guioperations.dto.TenantsResponse;

/**
 * Exposes the tenants (function groups) the current user has rights on, together with the organizations already
 * registered under each of them.
 *
 * @author Per Fredrik Plars
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tenants")
public class TenantController {

  private final TenantService tenantService;

  /**
   * Returns the tenants the current user has rights on, with the organizations already persisted under each.
   * Superusers get every tenant the registry is configured to support.
   *
   * @param authentication the current authentication
   * @return the tenants the caller has rights on, with the organizations registered under each
   */
  @GetMapping
  public TenantsResponse getTenants(final Authentication authentication) {
    return this.tenantService.resolveTenants(authentication);
  }
}