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

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

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
            .title("OpenID Federation Registry API")
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
                .description("JWT token obtained from authentication endpoint")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  /**
   * Post-processes the complete OpenAPI spec and injects {tenant} and {orgNumber} as
   * explicit path parameters on every operation whose path template contains them.
   * Springdoc does not reliably pick up class-level @RequestMapping path variables,
   * so this customizer adds them based on the path string directly.
   *
   * @return OpenAPI customizer
   */
  @Bean
  public OpenApiCustomizer tenantOrgNumberParameterCustomizer() {
    return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
      final boolean hasTenant = path.contains("{tenant}");
      final boolean hasOrgNumber = path.contains("{orgNumber}");
      if (!hasTenant && !hasOrgNumber) {
        return;
      }
      pathItem.readOperations().forEach(operation -> {
        List<Parameter> params = operation.getParameters();
        if (params == null) {
          params = new ArrayList<>();
          operation.setParameters(params);
        }
        final List<Parameter> paramsFinal = params;

        if (hasTenant && paramsFinal.stream().noneMatch(p -> "tenant".equals(p.getName()))) {
          paramsFinal.add(0, new Parameter()
              .name("tenant")
              .in("path")
              .required(true)
              .description("Tenant identifier, also the function name")
              .example("swedenconnect")
              .schema(new StringSchema()));
        }
        if (hasOrgNumber && paramsFinal.stream().noneMatch(p -> "orgNumber".equals(p.getName()))) {
          paramsFinal.add(hasTenant ? 1 : 0, new Parameter()
              .name("orgNumber")
              .in("path")
              .required(true)
              .description("Organization number")
              .example("2021003948")
              .schema(new StringSchema()));
        }
      });
    });
  }
}

