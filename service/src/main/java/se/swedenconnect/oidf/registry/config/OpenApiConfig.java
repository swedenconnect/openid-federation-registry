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

package se.swedenconnect.oidf.registry.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 *
 * @author Per Fredrik Plars
 */
@Configuration
public class OpenApiConfig {

  /**
   * Configures the OpenAPI documentation for the Entity Registry API.
   *
   * @return configured OpenAPI instance
   */
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("OpenID Federation Entity Registry API")
            .description("REST API for managing OpenID Federation entities, policies, modules, and trust marks. "
                + "This API provides endpoints for creating, reading, updating, and deleting federation entities, "
                + "policies, trust anchors, resolvers, trust marks, and trust mark subjects.")
            .version("0.5.10-SNAPSHOT")
            .contact(new Contact()
                .name("Sweden Connect")
                .url("https://www.swedenconnect.se")
                .email("support@swedenconnect.se"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
        .components(new Components()
            .addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtained from authentication endpoint"))
            .addSecuritySchemes("selectedOrgNumber", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("selected-org-number")
                .description("Organization number to use for the request. Enter your organization number here.")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth").addList("selectedOrgNumber"));
  }
}

