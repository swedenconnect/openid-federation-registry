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
package se.swedenconnect.oidf.registry.registrationflow.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configuration of the Metadata Policy Creation step: per entity type, the rules that decide what goes into the
 * Subordinate Statement's {@code metadata} and {@code metadata_policy} claims, and what is checked first.
 *
 * @param defaultLanguage the language whose value is copied to the parameter without a language tag
 * @param languages the language tags a multilingual parameter must be given in
 * @param onFailure what happens when a required value is missing or a check fails
 * @param entityTypes the rules, keyed by entity type such as {@code openid_relying_party}
 * @author Felix Hellman
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetadataPolicyTemplate(
    @JsonProperty("default_language") String defaultLanguage,
    @JsonProperty("languages") List<String> languages,
    @JsonProperty("on_failure") OnFailure onFailure,
    @JsonProperty("entity_types") Map<String, List<Rule>> entityTypes) {

  /** The operators of OpenID Federation 1.0, section 6.1.3.1. */
  public static final Set<String> OPERATORS =
      Set.of("value", "add", "default", "one_of", "subset_of", "superset_of", "essential");

  /**
   * What happens when a required value is missing or a check fails.
   *
   * @author Felix Hellman
   */
  public enum OnFailure {
    /** The step fails and the registration stops. */
    @JsonProperty("fail") FAIL,
    /** The step goes on; the failure is shown to the reviewer as a warning. */
    @JsonProperty("review") REVIEW
  }

  /**
   * Which claim a rule writes.
   *
   * @author Felix Hellman
   */
  public enum Claim {
    /** The value is set in the {@code metadata} claim. */
    @JsonProperty("metadata") METADATA,
    /** The value is constrained with an operator in the {@code metadata_policy} claim. */
    @JsonProperty("metadata_policy") METADATA_POLICY
  }

  /**
   * Where a rule's value comes from.
   *
   * @author Felix Hellman
   */
  public enum Source {
    /** The entity's own metadata. */
    @JsonProperty("entity") ENTITY,
    /** The registry's data about the organization behind the entity. */
    @JsonProperty("organization") ORGANIZATION,
    /** A fixed value given in the rule. */
    @JsonProperty("static") STATIC
  }

  /**
   * A field of the registry's data about an organization.
   *
   * @author Felix Hellman
   */
  public enum OrganizationField {
    /** The organization's legal name. */
    @JsonProperty("legal_name") LEGAL_NAME,
    /** The ten-digit organization number. */
    @JsonProperty("organization_number") ORGANIZATION_NUMBER,
    /** The organization number as a GLUE-URI, {@code urn:glue:iso6523:<icd>:<number>}. */
    @JsonProperty("organization_identifier") ORGANIZATION_IDENTIFIER
  }

  /**
   * Where the value without a language tag of a multilingual parameter comes from.
   *
   * @author Felix Hellman
   */
  public enum Untagged {
    /** No value without a language tag is written. */
    @JsonProperty("none") NONE,
    /** The value in the default language. */
    @JsonProperty("default_language") DEFAULT_LANGUAGE,
    /** The entity's own value without a language tag, else the value in the default language. */
    @JsonProperty("entity_or_default") ENTITY_OR_DEFAULT,
    /** A field of the registry's organization data. */
    @JsonProperty("organization") ORGANIZATION
  }

  /**
   * A check made on a rule's values before registration.
   *
   * @author Felix Hellman
   */
  public enum CheckType {
    /** Every value is a non-empty string. */
    @JsonProperty("non_empty") NON_EMPTY,
    /** Every value is an HTTPS URL. */
    @JsonProperty("https_url") HTTPS_URL,
    /** Every value is a URL that resolves to an image. */
    @JsonProperty("image") IMAGE,
    /** Every value equals a field of the registry's organization data. */
    @JsonProperty("matches_organization") MATCHES_ORGANIZATION,
    /** Every value is one of the allowed values. */
    @JsonProperty("allowed_values") ALLOWED_VALUES
  }

  /**
   * One rule: what to write for one parameter of one entity type.
   *
   * @param parameter the metadata parameter, without language tag
   * @param claim the claim the value is written to
   * @param operator the policy operator, for {@link Claim#METADATA_POLICY}
   * @param source where the value comes from
   * @param organizationField the organization field, for {@link Source#ORGANIZATION}
   * @param icd the ISO 6523 International Code Designator for a GLUE-URI, defaults to {@code 0007}
   * @param fallback another parameter to take the value from when the entity has not declared this one
   * @param staticValue the value, for {@link Source#STATIC}
   * @param required whether a missing value is a failure
   * @param multilingual whether the parameter is given per language
   * @param untagged where the value without a language tag comes from, for a multilingual parameter
   * @param untaggedOrganizationField the organization field, for {@link Untagged#ORGANIZATION}
   * @param checks the checks made on the values
   * @author Felix Hellman
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Rule(
      @JsonProperty("parameter") String parameter,
      @JsonProperty("claim") Claim claim,
      @JsonProperty("operator") String operator,
      @JsonProperty("source") Source source,
      @JsonProperty("organization_field") OrganizationField organizationField,
      @JsonProperty("icd") String icd,
      @JsonProperty("fallback") String fallback,
      @JsonProperty("static_value") Object staticValue,
      @JsonProperty("required") Boolean required,
      @JsonProperty("multilingual") Boolean multilingual,
      @JsonProperty("untagged") Untagged untagged,
      @JsonProperty("untagged_organization_field") OrganizationField untaggedOrganizationField,
      @JsonProperty("checks") List<Check> checks) {

    /**
     * Tells whether a missing value is a failure.
     *
     * @return {@code true} if the rule is required
     */
    @JsonIgnore
    public boolean isRequired() {
      return Boolean.TRUE.equals(this.required);
    }

    /**
     * Tells whether the parameter is given per language.
     *
     * @return {@code true} if the rule is multilingual
     */
    @JsonIgnore
    public boolean isMultilingual() {
      return Boolean.TRUE.equals(this.multilingual);
    }

    /**
     * Returns the operator, {@code value} when none is given.
     *
     * @return the operator
     */
    public String operatorOrValue() {
      return this.operator == null ? "value" : this.operator;
    }

    /**
     * Returns where the value without a language tag comes from, {@link Untagged#DEFAULT_LANGUAGE} when not given.
     *
     * @return the untagged setting
     */
    public Untagged untaggedOrDefault() {
      return this.untagged == null ? Untagged.DEFAULT_LANGUAGE : this.untagged;
    }

    /**
     * Returns the checks, never {@code null}.
     *
     * @return the checks
     */
    public List<Check> checksOrEmpty() {
      return this.checks == null ? List.of() : this.checks;
    }
  }

  /**
   * A check and its options.
   *
   * @param type the check
   * @param organizationField the organization field, for {@link CheckType#MATCHES_ORGANIZATION}
   * @param icd the ISO 6523 International Code Designator, for a GLUE-URI organization field
   * @param values the allowed values, for {@link CheckType#ALLOWED_VALUES}
   * @author Felix Hellman
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Check(
      @JsonProperty("type") CheckType type,
      @JsonProperty("organization_field") OrganizationField organizationField,
      @JsonProperty("icd") String icd,
      @JsonProperty("values") List<String> values) {
  }

  /**
   * Returns the default language, {@code sv} when none is given.
   *
   * @return the default language
   */
  public String defaultLanguageOrSv() {
    return this.defaultLanguage == null || this.defaultLanguage.isBlank() ? "sv" : this.defaultLanguage;
  }

  /**
   * Returns the required languages, Swedish and English when none are given.
   *
   * @return the languages
   */
  public List<String> languagesOrDefault() {
    return this.languages == null || this.languages.isEmpty() ? List.of("sv", "en") : this.languages;
  }

  /**
   * Returns what happens on a failure, {@link OnFailure#FAIL} when not given.
   *
   * @return the failure handling
   */
  public OnFailure onFailureOrFail() {
    return this.onFailure == null ? OnFailure.FAIL : this.onFailure;
  }

  /**
   * Lists what is wrong with this template. An empty list means the template can be used.
   *
   * @return the problems found, one message each
   */
  public List<String> problems() {
    final List<String> problems = new ArrayList<>();
    if (this.entityTypes == null || this.entityTypes.isEmpty()) {
      problems.add("entity_types must name at least one entity type");
      return problems;
    }
    this.entityTypes.forEach((entityType, rules) -> {
      if (rules == null) {
        problems.add("%s: rules must be a list".formatted(entityType));
        return;
      }
      for (int i = 0; i < rules.size(); i++) {
        ruleProblems("%s[%d]".formatted(entityType, i), rules.get(i), problems);
      }
    });
    return problems;
  }

  private static void ruleProblems(final String at, final Rule rule, final List<String> problems) {
    if (rule == null) {
      problems.add(at + ": rule is empty");
      return;
    }
    if (rule.parameter() == null || rule.parameter().isBlank() || rule.parameter().contains("#")) {
      problems.add(at + ": parameter must be given, without language tag");
    }
    if (rule.claim() == null) {
      problems.add(at + ": claim must be metadata or metadata_policy");
    }
    if (rule.source() == null) {
      problems.add(at + ": source must be entity, organization or static");
    }
    if (rule.claim() == Claim.METADATA && rule.operator() != null && !"value".equals(rule.operator())) {
      problems.add(at + ": a metadata rule sets the value and takes no operator");
    }
    if (rule.claim() == Claim.METADATA_POLICY && !OPERATORS.contains(rule.operatorOrValue())) {
      problems.add(at + ": operator must be one of " + OPERATORS);
    }
    if (rule.source() == Source.ORGANIZATION && rule.organizationField() == null) {
      problems.add(at + ": organization_field must be given for source organization");
    }
    if (rule.source() == Source.STATIC && rule.staticValue() == null) {
      problems.add(at + ": static_value must be given for source static");
    }
    if ("essential".equals(rule.operator()) && rule.source() == Source.STATIC
        && !(rule.staticValue() instanceof Boolean)) {
      problems.add(at + ": the essential operator takes true or false");
    }
    if (rule.isMultilingual() && rule.source() != Source.ENTITY) {
      problems.add(at + ": only values from the entity's metadata can be multilingual");
    }
    if (rule.isMultilingual() && rule.untaggedOrDefault() == Untagged.ORGANIZATION
        && rule.untaggedOrganizationField() == null) {
      problems.add(at + ": untagged_organization_field must be given for untagged organization");
    }
    if (rule.fallback() != null && rule.source() != Source.ENTITY) {
      problems.add(at + ": fallback only applies to values from the entity's metadata");
    }
    for (final Check check : rule.checksOrEmpty()) {
      if (check == null || check.type() == null) {
        problems.add(at + ": every check needs a type");
      }
      else if (check.type() == CheckType.MATCHES_ORGANIZATION && check.organizationField() == null) {
        problems.add(at + ": matches_organization needs organization_field");
      }
      else if (check.type() == CheckType.ALLOWED_VALUES && (check.values() == null || check.values().isEmpty())) {
        problems.add(at + ": allowed_values needs values");
      }
    }
  }
}
