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

package se.swedenconnect.oidf.registry.infrastructure.error;

import java.net.URI;

/**
 * Define error type constants
 *
 * @author Per Fredrik Plars
 */
public enum ErrorTypes {
  INVALID_PARAMETER("https://oidf.swedenconnect.se/error/invalid_parameter"),
  DATA_CONSTRAINT("https://oidf.swedenconnect.se/error/data_constraint"),
  RELATION_NOT_FOUND("https://oidf.swedenconnect.se/error/realtion_not_found"),
  NOT_FOUND("about:blank"),
  CONFLICT("about:blank"),
  BLANK("about:blank");

  public final URI errorURI;

  ErrorTypes(final String errorURI) {
    this.errorURI = URI.create(errorURI);
  }

}
