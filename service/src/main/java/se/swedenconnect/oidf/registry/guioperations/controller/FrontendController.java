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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The {@code FrontendController} class is responsible for routing unspecified application paths to the Vue 3 SPA
 * frontend.
 *
 * @author David Goldring
 */
@Slf4j
@Controller
public class FrontendController {

  /**
   * Forwards all requests that do not match anything in application to frontend
   *
   * @return a {@code String} indicating the forward path
   */
  @GetMapping("/login")
  public String forwardToFrontend() {
    return "forward:/";
  }
}
