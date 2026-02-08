/*
 * Copyright 2024-2025 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.registry.federation.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for assertions.
 *
 * @author Per Fredrik Plars
 */
public final class FederationAssert {
  /**
   * If value is empty an IllegalArgumentException is thrown with message
   *
   * @param value Value to be tested. If string isBlank is used. If list the isEmpty is used to test if it is empty
   * @param message Message in IllegalArgumentException
   * @param <V> Value to be tested
   * @return Value that is tested
   */
  public static <V> V assertNotEmpty(final V value, final String message) {
    return switch (value) {
      case null -> throw new IllegalArgumentException(message);
      case String s when s.isBlank() -> throw new IllegalArgumentException(message);
      case List list when list.isEmpty() -> throw new IllegalArgumentException(message);
      case Collection<?> objects when objects.isEmpty() -> throw new IllegalArgumentException(message);
      case Map<?, ?> objects when objects.isEmpty() -> throw new IllegalArgumentException(message);
      default -> value;
    };
  }

  /**
   * If value is empty exception will be thrown.
   *
   * @param value Value to be tested. If string isBlank is used. If list the isEmpty is used to test if it is empty
   * @param ex Supplier for the exception
   * @param <V> Value
   * @param <E> Exception
   * @return Value that is tested
   * @throws E Exception that is returned from suppler if value is empty
   */
  public static <V, E extends Exception> V assertNotEmptyThrows(final V value, final Supplier<E> ex) throws E {
    return switch (value) {
      case null -> throw ex.get();
      case String s when s.isBlank() -> throw ex.get();
      case List<?> list when list.isEmpty() -> throw ex.get();
      case Collection<?> objects when objects.isEmpty() -> throw ex.get();
      default -> value;
    };
  }

  /**
   * Throws IllegalArgumentException if value is false
   *
   * @param value Boolean value if true, exception is thrown
   * @param message Message in IllegalArgumentException
   */
  public static void assertTrue(final Boolean value, final String message) {
    assertNotEmpty(value, message);
    if (!value) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * If value is not null doOperation is called otherwise supplyer with exception
   *
   * @param value Value to be tested
   * @param doOperation Operation to be called if value is not null
   * @param ex Exception supplier
   * @param <V> Type of value object
   * @param <E> Type of exception
   * @throws E Exception thrown
   */
  public static <V, E extends Exception> void throwIfNull(final V value, final Consumer<V> doOperation,
      final Supplier<E> ex)
      throws E {
    assertNotEmptyThrows(value, ex);
    doOperation.accept(value);
  }

}