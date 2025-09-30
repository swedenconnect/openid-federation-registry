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
package se.swedenconnect.oidf.common.entity.entity.integration.registry.records;

/**
 * Field names for registry records.
 *
 * @author Felix Hellman
 */
public class RecordFields {
  /**
   * Field names for Entity Records.
   */
  public static class Entity {
    /**
     * metadata_policy_crit name.
     */
    public static final String METADATA_POLICY_CRIT = "metadata_policy_crit";
    /**
     * crit name.
     */
    public static final String CRIT = "crit";
    /**
     * Issuer field name.
     */
    public static final String ISSUER = "issuer";
    /**
     * Subject field name.
     */
    public static final String SUBJECT = "subject";
    /**
     * Policy Record field name.
     */
    public static final String POLICY_RECORD = "policy_record";
    /**
     * Hosted Record field name.
     */
    public static final String HOSTED_RECORD = "hosted_record";
    /**
     * Override Configuration Location field name.
     */
    public static final String OVERRIDE_CONFIGURATION_LOCATION = "override_configuration_location";
    /**
     * JWKS field name.
     */
    public static final String JWKS = "jwks";
  }

  /**
   * Trust Mark Source Record field names.
   */
  public static class TrustMarkSource {
    /**
     * Issuer field name.
     */
    public static final String ISSUER = "issuer";
    /**
     * Trust mark id field name.
     */
    public static final String TRUST_MARK_ID = "trust_mark_id";
  }

  /**
   * Hosted Record field names.
   */
  public static class HostedRecord {
    /**
     * Authority hints field name.
     */
    public static final String AUTHORITY_HINTS = "authority_hints";
    /**
     * Metadata field name.
     */
    public static final String METADATA = "metadata";
    /**
     * Trust mark sources.
     */
    public static final String TRUST_MARK_SOURCES = "trust_mark_sources";
  }

  /**
   * Module record field names.
   */
  public static class Modules {
    /**
     * Resolvers field name.
     */
    public static final String RESOLVERS = "resolvers";
    /**
     * Trust Anchors field name.
     */
    public static final String TRUST_ANCHORS = "trust_anchors";
    /**
     * Trust Mark issuers field name.
     */
    public static final String TRUST_MARK_ISSUERS = "trust_mark_issuers";
  }

  /**
   * Policy record field names
   */
  public static class Policy {
    /**
     * Policy record id field name.
     */
    public static final String POLICY_RECORD_ID = "policy_record_id";
    /**
     * Policy field name.
     */
    public static final String POLICY = "policy";
  }

  /**
   * Trust mark record field names.
   */
  public static class TrustMark {
    /**
     * Trust mark issuer field name
     */
    public static final String TRUST_MARK_ISSUER_ID = "trust_mark_issuer_id";
    /**
     * Trust mark id field name
     */
    public static final String TRUST_MARK_ID = "trust_mark_id";
    /**
     * Delegation field name
     */
    public static final String DELEGATION = "delegation";
    /**
     * Ref field name
     */
    public static final String REF = "ref";
    /**
     * Logo uri field name
     */
    public static final String LOGO_URI = "logo_uri";
    /**
     * Subjects Field Name
     */
    public static final String SUBJECTS = "subjects";
  }

  /**
   * Resolver Module Record fields.
   */
  public static class ResolverModule {
    /**
     * Trust anchors field.
     */
    public static final String TRUST_ANCHORS = "trust_anchors";
    /**
     * Resolver response duration field.
     */
    public static final String RESOLVE_RESPONSE_DURATION = "resolve_response_duration";
    /**
     * Trusted Keys field.
     */
    public static final String TRUSTED_KEYS = "trusted_keys";
    /**
     * Entity Identifier Field.
     */
    public static final String ENTITY_IDENTIFIER = "entity_identifier";
    /**
     * Step Retry Time Field.
     */
    public static final String STEP_RETRY_TIME = "step_retry_time";
    /**
     * Step cached value threshold field.
     */
    public static final String STEP_CACHED_VALUE_THRESHOLD = "step_cached_value_threshold";
  }

  /**
   * Trust Anchor Module Record Fields.
   */
  public static class TrustAnchorModule {
    /**
     * Entity Identifier Field.
     */
    public static final String ENTITY_IDENTIFIER = "entity_identifier";

    /**
     * Trust Mark Issuers Field.
     */
    public static final String TRUST_MARK_ISSUERS = "trust_mark_issuers";
  }

  /**
   * Trust Mark Subject Fields.
   */
  public static class TrustMarkSubject {
    /**
     * Subject field.
     */
    public static final String SUBJECT = "subject";
    /**
     * Revoked field.
     */
    public static final String REVOKED = "revoked";
    /**
     * Expires field.
     */
    public static final String EXPIRES = "expires";
    /**
     * Granted field.
     */
    public static final String GRANTED = "granted";
  }

  /**
   * Trust Mark Issuer Module Record Fields.
   */
  public static class TrustMarkIssuerModule {
    /**
     * Entity identifier field.
     */
    public static final String ENTITY_IDENTIFIER = "entity_identifier";
    /**
     * Trust Mark Token Validity Duration Field.
     */
    public static final String TRUST_MARK_TOKEN_VALIDITY_DURATION = "trust_mark_token_validity_duration";
    /**
     * Trust Marks Field.
     */
    public static final String TRUST_MARKS = "trust_marks";
    /**
     * Trust Mark Validity Duration Field.
     */
    public static final String TRUST_MARK_VALIDITY_DURATION = "trust_mark_validity_duration";
  }
}
