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
 *  limitations under the License.
 */
package se.swedenconnect.oidf.registry.infrastructure.auth.domain;

/**
 * A single function+right entry within an {@link OrgRightEntry}.
 *
 * @param function the function group name this right applies to
 * @param right    the granted right level
 * @author Per Fredrik Plars
 */
public record FunctionRight(String function, Right right) {
}
