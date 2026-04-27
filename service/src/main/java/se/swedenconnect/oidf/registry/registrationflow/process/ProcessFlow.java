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

import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * ProcessFlow definition
 *
 * @author Per Fredrik Plars
 */
@Getter
public class ProcessFlow implements Serializable {

  UUID flowId;
  String name;
  String description;
  List<StepDefinition> processFlow;

  /**
   *
   * @param flowId
   * @param name
   * @param description
   * @param processFlow
   */
  public ProcessFlow(final UUID flowId,
      final String name,
      final String description,
      final List<StepDefinition> processFlow) {
    this.flowId = flowId;
    this.name = name;
    this.description = description;
    this.processFlow = processFlow;
  }
}
