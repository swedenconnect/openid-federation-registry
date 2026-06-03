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

/**
 * Well-known keys for values stored in a {@link ProcessContext}.
 * <p>
 * Using string constants here keeps the API simple; a typed {@code ContextKey<T>} variant
 * can be introduced later if cast-safety becomes a concern.
 *
 * @author Per Fredrik Plars
 */
public final class ContextKey {

  public static final String ENTITY_CONFIGURATION_METADATA = "entity_configuration_metadata";
  public static final String ENTITY_CONFIGURATION_JWKS = "entity_configuration_jwks";
  public static final String REGISTRATION_ID = "registration_id";
  public static final String ENTITY_ID = "entityId";
  public static final String METADATA_POLICY = "metadata_policy";
  public static final String REGISTRATION_POLICIES = "registration_policies";
  public static final String METADATA_POLICY_TEMPLATE = "metadata_policy_template";
  public static final String TAIM_ID = "taimId";
  public static final String JOIN_ID = "joinId";
  public static final String TRUSTMARKS_REQUESTED = "trustmarksRequested";
  public static final String ORG = "org";
  public static final String REQUEST_METADATA = "request_metadata";
  public static final String TRUSTMARK_ISSUER_ID = "trustmarkIssuerId";
  public static final String STEP_APPROVED = "stepApproved";
  public static final String TRUSTMARK_SUBJECT_PROCEED = "trustmarkSubjectProceed";

  private ContextKey() {
  }
}
