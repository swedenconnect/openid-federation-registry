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
package se.swedenconnect.oidf.registry.infrastructure.audit;

/**
 * Enumerates the types of events that can be audited within a registry system. This enumeration supports both write
 * operations (e.g., create, update, delete) and read operations made through the federation API.
 *
 * @author Per Fredrik Plars
 */
public enum RegistryAuditEventType {

  POLICY_CREATED,
  POLICY_UPDATED,
  POLICY_DELETED,
  FEDERATION_ENTITY_CREATED,
  FEDERATION_ENTITY_UPDATED,
  FEDERATION_ENTITY_DELETED,
  HOSTED_ENTITY_CREATED,
  HOSTED_ENTITY_UPDATED,
  HOSTED_ENTITY_DELETED,
  SUBORDINATE_ENTITY_CREATED,
  SUBORDINATE_ENTITY_UPDATED,
  SUBORDINATE_ENTITY_DELETED,
  TRUST_ANCHOR_CREATED,
  TRUST_ANCHOR_UPDATED,
  TRUST_ANCHOR_DELETED,
  INTERMEDIATE_CREATED,
  INTERMEDIATE_UPDATED,
  INTERMEDIATE_DELETED,
  RESOLVER_CREATED,
  RESOLVER_UPDATED,
  RESOLVER_DELETED,
  TRUSTMARK_CREATED,
  TRUSTMARK_UPDATED,
  TRUSTMARK_DELETED,
  TRUSTMARK_SUBJECT_CREATED,
  TRUSTMARK_SUBJECT_UPDATED,
  TRUSTMARK_SUBJECT_DELETED,
  TRUSTMARK_ISSUER_CREATED,
  TRUSTMARK_ISSUER_UPDATED,
  TRUSTMARK_ISSUER_DELETED,
  SUBORDINATE_CREATED,
  SUBORDINATE_UPDATED,
  SUBORDINATE_DELETED,
  RESOLVED_ENTITY_CONFIGURATION
}
