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

import net.minidev.json.JSONValue;
import se.swedenconnect.oidf.registry.registrationflow.metadata.MetadataPolicyTemplate.Check;
import se.swedenconnect.oidf.registry.registrationflow.metadata.MetadataPolicyTemplate.Rule;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Applies a {@link MetadataPolicyTemplate} to an entity's metadata: works out the values of the Subordinate
 * Statement's {@code metadata} and {@code metadata_policy} claims and runs the template's checks. Has no side effects;
 * images and JSON documents are looked up through the functions it is given.
 *
 * @author Felix Hellman
 */
public final class MetadataPolicyGenerator {

  private MetadataPolicyGenerator() {
  }

  /**
   * The outcome of applying a template.
   *
   * @param metadata the {@code metadata} claim, keyed by entity type
   * @param metadataPolicy the {@code metadata_policy} claim, keyed by entity type
   * @param checks every check made, passed or failed, in rule order
   * @param pinned the values fixed with the {@code value} operator, for the reviewer
   * @author Felix Hellman
   */
  public record Result(
      Map<String, Object> metadata,
      Map<String, Object> metadataPolicy,
      List<PreviewSection.Check> checks,
      List<PreviewSection.Field> pinned) {

    /**
     * Lists the checks that failed.
     *
     * @return the failed checks' messages
     */
    public List<String> failures() {
      return this.checks.stream()
          .filter(check -> check.status() == PreviewSection.CheckStatus.FAIL)
          .map(PreviewSection.Check::message)
          .toList();
    }
  }

  /**
   * Applies a template, with no JSON documents to look up. Only the entity types the entity declares are considered.
   *
   * @param template the template
   * @param entityMetadata the entity's metadata, keyed by entity type
   * @param organization the registry's data about the organization behind the entity
   * @param images looks up an image URL; used by the {@code image} check
   * @return the claims and the check outcomes
   */
  public static Result generate(final MetadataPolicyTemplate template, final Map<String, Object> entityMetadata,
      final OrganizationData organization, final Function<String, FetchedResource> images) {
    return generate(template, entityMetadata, organization, images,
        url -> FetchedResource.notFetched(url, "Not fetched"));
  }

  /**
   * Applies a template. Only the entity types the entity declares are considered.
   *
   * @param template the template
   * @param entityMetadata the entity's metadata, keyed by entity type
   * @param organization the registry's data about the organization behind the entity
   * @param images looks up an image URL; used by the {@code image} check
   * @param documents looks up a JSON document URL; used by the {@code lists_redirect_uris} check
   * @return the claims and the check outcomes
   */
  public static Result generate(final MetadataPolicyTemplate template, final Map<String, Object> entityMetadata,
      final OrganizationData organization, final Function<String, FetchedResource> images,
      final Function<String, FetchedResource> documents) {
    final Map<String, Object> metadata = new LinkedHashMap<>();
    final Map<String, Object> metadataPolicy = new LinkedHashMap<>();
    final List<PreviewSection.Check> checks = new ArrayList<>();
    final List<PreviewSection.Field> pinned = new ArrayList<>();

    template.entityTypes().forEach((entityType, rules) -> {
      if (!(entityMetadata.get(entityType) instanceof final Map<?, ?> declared)) {
        return;
      }
      final Map<String, Object> typeMetadata = new LinkedHashMap<>();
      final Map<String, Object> typePolicy = new LinkedHashMap<>();
      for (final Rule rule : rules) {
        final Context context = new Context(template, declared, organization, images, documents, checks);
        final Map<String, Object> values = context.values(rule);
        if (values.isEmpty()) {
          continue;
        }
        context.check(rule, values);
        values.forEach((suffix, value) -> {
          final String name = rule.parameter() + suffix;
          if (rule.claim() == MetadataPolicyTemplate.Claim.METADATA) {
            typeMetadata.put(name, value);
          }
          else {
            typePolicy.put(name, Map.of(rule.operatorOrValue(), value));
            if ("value".equals(rule.operatorOrValue())) {
              pinned.add(new PreviewSection.Field(name, render(value)));
            }
          }
        });
      }
      if (!typeMetadata.isEmpty()) {
        metadata.put(entityType, typeMetadata);
      }
      if (!typePolicy.isEmpty()) {
        metadataPolicy.put(entityType, typePolicy);
      }
    });
    return new Result(metadata, metadataPolicy, List.copyOf(checks), List.copyOf(pinned));
  }

  private static String render(final Object value) {
    return value instanceof final String text ? text : JSONValue.toJSONString(value);
  }

  /**
   * Works out and checks the values of one rule.
   *
   * @param template the template the rule belongs to
   * @param declared the entity's metadata for the rule's entity type
   * @param organization the registry's data about the organization
   * @param images looks up an image URL
   * @param documents looks up a JSON document URL
   * @param checks where check outcomes are collected
   */
  private record Context(MetadataPolicyTemplate template, Map<?, ?> declared, OrganizationData organization,
      Function<String, FetchedResource> images, Function<String, FetchedResource> documents,
      List<PreviewSection.Check> checks) {

    private void ok(final String message) {
      this.checks.add(new PreviewSection.Check(PreviewSection.CheckStatus.OK, message));
    }

    private void fail(final String message) {
      this.checks.add(new PreviewSection.Check(PreviewSection.CheckStatus.FAIL, message));
    }

    private void info(final String message) {
      this.checks.add(new PreviewSection.Check(PreviewSection.CheckStatus.INFO, message));
    }

    private boolean declares(final String parameter) {
      return this.declared.keySet().stream()
          .map(String::valueOf)
          .anyMatch(key -> key.equals(parameter) || key.startsWith(parameter + "#"));
    }

    /**
     * Works out a rule's values, keyed by language tag suffix ({@code ""} for no tag). Records a failure for a
     * required value that is missing. Returns an empty map when there is nothing to write.
     */
    private Map<String, Object> values(final Rule rule) {
      final Map<String, Object> values = new LinkedHashMap<>();
      final String parameter = rule.parameter();
      switch (rule.source()) {
      case ORGANIZATION -> putIfPresent(values, "", this.organization.value(rule.organizationField(), rule.icd()));
      case STATIC -> putIfPresent(values, "", rule.staticValue());
      case ENTITY -> {
        String from = parameter;
        if (!this.declares(parameter) && rule.fallback() != null && this.declares(rule.fallback())) {
          from = rule.fallback();
          this.info("%s is not declared; taken from %s".formatted(parameter, from));
        }
        if (rule.isMultilingual()) {
          return this.multilingualValues(rule, from);
        }
        putIfPresent(values, "", this.declared.get(from));
      }
      }
      if (values.isEmpty() && rule.isRequired()) {
        this.fail("%s is required but missing".formatted(parameter));
      }
      return values;
    }

    private Map<String, Object> multilingualValues(final Rule rule, final String from) {
      final Map<String, Object> values = new LinkedHashMap<>();
      final List<String> missing = new ArrayList<>();
      for (final String language : this.template.languagesOrDefault()) {
        if (!putIfPresent(values, "#" + language, this.declared.get(from + "#" + language))) {
          missing.add("#" + language);
        }
      }
      if (values.isEmpty() && this.declared.get(from) == null) {
        if (rule.isRequired()) {
          this.fail("%s is required but missing".formatted(rule.parameter()));
        }
        return Map.of();
      }
      if (!missing.isEmpty()) {
        this.fail("%s is missing language %s".formatted(rule.parameter(), String.join(", ", missing)));
      }
      final Object defaultLanguageValue = values.get("#" + this.template.defaultLanguageOrSv());
      switch (rule.untaggedOrDefault()) {
      case NONE -> { }
      case DEFAULT_LANGUAGE -> putIfPresent(values, "", defaultLanguageValue);
      case ENTITY_OR_DEFAULT -> {
        final Object own = nonBlank(this.declared.get(from));
        putIfPresent(values, "", own != null ? own : defaultLanguageValue);
      }
      case ORGANIZATION -> {
        if (!putIfPresent(values, "", this.organization.value(rule.untaggedOrganizationField(), null))) {
          this.fail("%s: the registry holds no %s for the organization".formatted(rule.parameter(),
              rule.untaggedOrganizationField().name().toLowerCase()));
        }
      }
      }
      return values;
    }

    private void check(final Rule rule, final Map<String, Object> values) {
      final List<String> texts = values.values().stream()
          .flatMap(value -> value instanceof final Collection<?> list ? list.stream() : Stream.of(value))
          .map(value -> value instanceof final String text ? text : render(value))
          .toList();
      final String parameter = rule.parameter();
      for (final Check check : rule.checksOrEmpty()) {
        switch (check.type()) {
        case NON_EMPTY -> {
          if (texts.stream().allMatch(text -> !text.isBlank())) {
            this.ok("%s is not empty".formatted(parameter));
          }
          else {
            this.fail("%s is empty".formatted(parameter));
          }
        }
        case HTTPS_URL -> texts.forEach(text -> {
          if (text.regionMatches(true, 0, "https://", 0, 8)) {
            this.ok("%s is an HTTPS URL".formatted(parameter));
          }
          else {
            this.fail("%s is not an HTTPS URL: %s".formatted(parameter, text));
          }
        });
        case IMAGE -> texts.forEach(text -> {
          final FetchedResource image = this.images.apply(text);
          if (image.isImage()) {
            this.ok("%s resolves to an image (%s, %d KB)".formatted(parameter, image.contentType(),
                Math.max(1, image.size() / 1024)));
          }
          else {
            this.fail("%s does not resolve to an image: %s".formatted(parameter, image.error()));
          }
        });
        case LISTS_REDIRECT_URIS -> texts.forEach(text -> this.listsRedirectUris(parameter, text));
        case MATCHES_ORGANIZATION -> {
          final String expected = this.organization.value(check.organizationField(), check.icd());
          if (expected != null && texts.stream().allMatch(expected::equals)) {
            this.ok("%s matches the organization's %s".formatted(parameter, label(check)));
          }
          else {
            this.fail("%s does not match the organization's %s: expected %s".formatted(parameter, label(check),
                expected == null ? "a value the registry does not hold" : expected));
          }
        }
        case ALLOWED_VALUES -> {
          final List<String> notAllowed = texts.stream().filter(text -> !check.values().contains(text)).toList();
          if (notAllowed.isEmpty()) {
            this.ok("%s holds only allowed values".formatted(parameter));
          }
          else {
            this.fail("%s holds values that are not allowed: %s".formatted(parameter,
                String.join(", ", notAllowed)));
          }
        }
        }
      }
    }

    /**
     * Checks that a URL resolves to a JSON array that lists every value of the entity's {@code redirect_uris}.
     */
    private void listsRedirectUris(final String parameter, final String url) {
      final FetchedResource document = this.documents.apply(url);
      if (document.error() != null || document.dataUri() == null) {
        this.fail("%s does not resolve to a JSON document: %s".formatted(parameter,
            document.error() == null ? "Not fetched" : document.error()));
        return;
      }
      final String json = new String(Base64.getDecoder().decode(
          document.dataUri().substring(document.dataUri().indexOf(',') + 1)), StandardCharsets.UTF_8);
      if (!(JSONValue.parse(json) instanceof final List<?> listed)) {
        this.fail("%s does not resolve to a JSON array".formatted(parameter));
        return;
      }
      if (!(this.declared.get("redirect_uris") instanceof final Collection<?> redirectUris)
          || redirectUris.isEmpty()) {
        this.fail("%s cannot be checked: the entity declares no redirect_uris".formatted(parameter));
        return;
      }
      final List<String> missing = redirectUris.stream()
          .filter(redirectUri -> !listed.contains(redirectUri))
          .map(String::valueOf)
          .toList();
      if (missing.isEmpty()) {
        this.ok("%s lists all redirect_uris".formatted(parameter));
      }
      else {
        this.fail("%s does not list these redirect_uris: %s".formatted(parameter, String.join(", ", missing)));
      }
    }

    private static String label(final Check check) {
      return check.organizationField().name().toLowerCase().replace('_', ' ');
    }

    private static Object nonBlank(final Object value) {
      return value instanceof final String text && text.isBlank() ? null : value;
    }

    private static boolean putIfPresent(final Map<String, Object> values, final String suffix, final Object value) {
      if (value == null || value instanceof final String text && text.isBlank()) {
        return false;
      }
      values.put(suffix, value);
      return true;
    }
  }
}
