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

import jakarta.servlet.ServletContext;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Controller for templating index.html for setting base path.
 *
 * @author Felix Hellman
 */
@RestController
public class IndexController {

  @Getter
  private final String contextPath;

  /**
   * Constructor
   *
   * @param context for reading context-path
   */
  public IndexController(final ServletContext context) {
    final StringBuilder contextPathBuilder = new StringBuilder(context.getContextPath());
    if (!context.getContextPath().endsWith("/")) {
      contextPathBuilder.append("/");
    }
    this.contextPath = contextPathBuilder.toString();
  }

  private static final String ELEMENT = """
      <base id="base-href-id" href="/"/>
      """;

  private static final String REPLACE_ELEMENT_FORMAT = """
      <base id="base-href-id" href="%s"/>
      """;

  /**
   * @return modified index.html with basepath set to context-path if context-path is set.
   * @throws IOException if index.html is not found
   */
  @GetMapping("/")
  public String baseIndexTemplate() throws IOException {
    final byte[] bytes = new ClassPathResource("static/index.html").getInputStream().readAllBytes();
    final String file = new String(bytes);
    if (this.shouldReplaceContextPath()) {
      return file.replace(ELEMENT, REPLACE_ELEMENT_FORMAT.formatted(this.contextPath));
    }
    return file;
  }

  boolean shouldReplaceContextPath() {
    return !this.contextPath.equals("/") && !this.contextPath.isEmpty();
  }
}
