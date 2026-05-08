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
package se.swedenconnect.oidf.registry.guioperations.dto;

import lombok.Data;

import java.util.Map;

/**
 * DTO carrying the decoded header and payload of an entity configuration JWT.
 * The signature is intentionally omitted.
 *
 * @author Per Fredrik Plars
 */
@Data
public class EntityConfigurationViewDto {

  /** JWT header fields (alg, kid, typ, …). */
  private Map<String, Object> header;

  /** JWT payload / claims (sub, iss, iat, exp, jwks, metadata, …). */
  private Map<String, Object> payload;
}
