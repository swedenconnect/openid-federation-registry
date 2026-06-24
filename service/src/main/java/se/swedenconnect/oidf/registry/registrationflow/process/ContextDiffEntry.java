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
package se.swedenconnect.oidf.registry.registrationflow.process;

/**
 * Describes a single change to the pipeline context caused by a step execution.
 *
 * @param key context key that changed
 * @param changeType one of {@code ADDED}, {@code CHANGED}, or {@code REMOVED}
 * @param before string representation of the value before execution, or {@code null} if added
 * @param after string representation of the value after execution, or {@code null} if removed
 * @author Felix Hellman
 */
public record ContextDiffEntry(String key, String changeType, String before, String after) {
}
