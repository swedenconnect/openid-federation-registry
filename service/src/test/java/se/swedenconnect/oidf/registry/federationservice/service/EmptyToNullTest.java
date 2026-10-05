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
package se.swedenconnect.oidf.registry.federationservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class EmptyToNullTest {

  @Test
  void emptyListBecomesNull() {
    assertThat(EmptyToNull.list(new ArrayList<String>())).isNull();
    assertThat(EmptyToNull.list(List.of())).isNull();
  }

  @Test
  void nullStaysNull() {
    assertThat(EmptyToNull.list(null)).isNull();
  }

  @Test
  void listWithContentIsKept() {
    assertThat(EmptyToNull.list(List.of("ec_location"))).containsExactly("ec_location");
  }
}
