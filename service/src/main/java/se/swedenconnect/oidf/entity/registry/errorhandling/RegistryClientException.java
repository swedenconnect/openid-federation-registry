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

package se.swedenconnect.oidf.entity.registry.errorhandling;

import lombok.Getter;

import java.net.URI;

/**
 * Represents a custom exception for registry client errors. This exception is used to encapsulate error information
 * related to operations in the registry client and includes specific error types as a URI.
 *
 * @author Per Fredrik Plars
 */
@Getter
public class RegistryClientException extends RuntimeException {
  final URI errorTypes;

  /**
   * Constructs a new {@code RegistryClientException} with the specified error type URI and message.
   *
   * @param errorTypes the URI representing specific error types associated with this exception
   * @param message the detailed message explaining the reason for this exception
   */
  public RegistryClientException(final URI errorTypes, final String message) {
    super(message);
    this.errorTypes = errorTypes;
  }

  /**
   * Constructs a new {@code RegistryClientException} with the specified error type URI, message, and cause.
   *
   * @param errorTypes the URI representing specific error types associated with this exception
   * @param message the detailed message explaining the reason for this exception
   * @param cause the underlying cause of this exception
   */
  public RegistryClientException(final URI errorTypes, final String message, final Throwable cause) {
    super(message, cause);
    this.errorTypes = errorTypes;
  }
}
