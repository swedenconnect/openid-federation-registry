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

package se.swedenconnect.oidf.registry.registrations.dto;

/**
 * Tags for a registration
 *
 * @author Per Fredrik Plars
 */
public enum RegistrationTagsDto {
  OIDC,
  SAML,
  HOSTED,
  FED,   // federation_entity
  RP,    // openid_relying_party
  OP,    // openid_provider
  AS,   // oauth_authorization_server
  OAC,   // oauth_client
  ORS    // oauth_resource / oauth_resource_server
}
