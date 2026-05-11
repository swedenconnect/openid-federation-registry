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
package se.swedenconnect.oidf.registry.registrationflow.process;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An {@link ArrayList} whose element type is bounded to {@link Serializable}, making the list
 * itself eligible for storage in {@link ProcessContext}.
 *
 * @param <E> element type
 * @author Per Fredrik Plars
 */
public class SerializableList<E extends Serializable> extends ArrayList<E> {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs an empty list.
   */
  public SerializableList() {
    super();
  }

  /**
   * Constructs a list containing the elements of the given collection.
   *
   * @param c source collection
   */
  public SerializableList(final Collection<? extends E> c) {
    super(c);
  }
}