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
package se.swedenconnect.oidf.registry.registrationflow.process.step.impl;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.federation.policy.MetadataPolicy;
import com.nimbusds.openid.connect.sdk.federation.policy.language.PolicyViolationException;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.organization.model.Organization;
import se.swedenconnect.oidf.registry.organization.service.OrganizationService;
import se.swedenconnect.oidf.registry.registrationflow.metadata.FetchedResource;
import se.swedenconnect.oidf.registry.registrationflow.metadata.MetadataPolicyGenerator;
import se.swedenconnect.oidf.registry.registrationflow.metadata.MetadataPolicyTemplate;
import se.swedenconnect.oidf.registry.registrationflow.metadata.OrganizationData;
import se.swedenconnect.oidf.registry.registrationflow.metadata.ResourceFetcher;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds the {@code metadata} and {@code metadata_policy} claims of the Subordinate Statement from a configurable
 * template, see {@link MetadataPolicyTemplate}. A step with no template uses the default template.
 *
 * <p>The claims are built and checked in {@code buildContext}, so an operator reviewing this step sees exactly what
 * will be published and which checks passed. A failed check either fails the step or, with
 * {@code on_failure: review}, is shown to the reviewer as a warning.</p>
 *
 * @author Felix Hellman
 */
@Component
public class MetadataPolicyCreationStep extends NoConfigStepAdapter implements ReviewableStep {

  /** Name of the configuration value holding the template, as JSON. */
  public static final String TEMPLATE = "template";

  private static final String PRESET_RESOURCE = "/metadata-policy/default.json";

  private final JsonMapper jsonMapper;
  private final OrganizationService organizationService;
  private final ResourceFetcher resourceFetcher;
  private final String presetTemplate;

  /**
   * Constructor.
   *
   * @param jsonMapper mapper used to read templates
   * @param organizationService service resolving the registry's data about the organization behind the entity
   * @param resourceFetcher fetcher for images and JSON documents the metadata points to
   */
  public MetadataPolicyCreationStep(final JsonMapper jsonMapper, final OrganizationService organizationService,
      final ResourceFetcher resourceFetcher) {
    this.jsonMapper = jsonMapper;
    this.organizationService = organizationService;
    this.resourceFetcher = resourceFetcher;
    try (InputStream preset = Objects.requireNonNull(
        MetadataPolicyCreationStep.class.getResourceAsStream(PRESET_RESOURCE), PRESET_RESOURCE)) {
      this.presetTemplate = new String(preset.readAllBytes(), StandardCharsets.UTF_8);
    }
    catch (final IOException e) {
      throw new UncheckedIOException("Cannot read the default metadata policy template", e);
    }
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public String getDescription() {
    return "Builds the metadata and metadata_policy claims of the subordinate statement from a template, and checks "
        + "the entity's metadata against it.";
  }

  @Override
  public StepResult buildContext(final ProcessContext ctx, final StepConfig config) {
    final Evaluation evaluation = this.evaluate(ctx, config);
    if (evaluation.failed()) {
      return StepResult.failure("Metadata checks failed", evaluation.issues(Severity.ERROR));
    }
    return StepResult.success("Metadata and metadata policy built");
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    final Evaluation evaluation = this.evaluate(ctx, config);
    if (evaluation.failed()) {
      return StepResult.failure("Metadata checks failed", evaluation.issues(Severity.ERROR));
    }
    if (!evaluation.result().failures().isEmpty()) {
      return StepResult.warning("Metadata and metadata policy built; checks failed and were accepted on review",
          evaluation.issues(Severity.WARNING));
    }
    return StepResult.success("Metadata and metadata policy built");
  }

  @Override
  public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
    final MetadataPolicyGenerator.Result result = this.evaluate(ctx, config).result();
    final Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("metadata", result.metadata());
    claims.put("metadata_policy", result.metadataPolicy());
    return StepPreview.builder()
        .add(PreviewSection.checks("Checks", result.checks()))
        .add(result.pinned().isEmpty() ? null : PreviewSection.fields("Pinned values", result.pinned()))
        .add(PreviewSection.json("Subordinate Statement claims", claims))
        .build();
  }

  /**
   * Builds the claims and runs the checks as {@code buildContext} does, without failing: for a dry run of the step.
   *
   * @param ctx the context, which must hold the entity's metadata; the claims are put in it
   * @param config the step's configuration
   * @return the claims, the check outcomes and every problem
   */
  public Evaluation dryRun(final ProcessContext ctx, final StepConfig config) {
    return this.evaluate(ctx, config);
  }

  /**
   * Applies the configured template to the entity's metadata and puts the claims in the context. Images and JSON
   * documents are fetched once per run and kept in the context, so evaluating again gives the same outcome.
   */
  @SuppressWarnings("unchecked")
  private Evaluation evaluate(final ProcessContext ctx, final StepConfig config) {
    final JSONObject entityMetadata = ctx.getRequired(ContextKey.ENTITY_CONFIGURATION_METADATA, JSONObject.class);
    final OrganizationRecord organization = ctx.getRequired(ContextKey.ORG);
    final MetadataPolicyTemplate template = this.parse(this.templateJson(config));
    final OrganizationData organizationData = new OrganizationData(
        this.organizationService.find(organization).map(Organization::getLegalName).orElse(null),
        organization.orgNumber());

    final JSONObject fetched = ctx.<JSONObject>get(ContextKey.FETCHED_RESOURCES).orElseGet(JSONObject::new);
    final MetadataPolicyGenerator.Result result = MetadataPolicyGenerator.generate(template, entityMetadata,
        organizationData, url -> {
          if (fetched.get(url) instanceof final Map<?, ?> known) {
            return FetchedResource.fromMap(known);
          }
          final FetchedResource resource = this.resourceFetcher.fetchImage(url, organization);
          fetched.put(url, resource.toMap());
          return resource;
        }, url -> {
          if (fetched.get(url) instanceof final Map<?, ?> known) {
            return FetchedResource.fromMap(known);
          }
          final FetchedResource resource = this.resourceFetcher.fetchJson(url, organization);
          fetched.put(url, resource.toMap());
          return resource;
        });
    if (!fetched.isEmpty()) {
      ctx.put(ContextKey.FETCHED_RESOURCES, fetched);
    }

    final List<String> problems = new ArrayList<>(result.failures());
    result.metadataPolicy().forEach((entityType, policy) -> {
      try {
        MetadataPolicy.parse(new JSONObject((Map<String, Object>) policy));
      }
      catch (final ParseException | PolicyViolationException | ClassCastException e) {
        problems.add("The template produces an invalid metadata policy for %s: %s".formatted(entityType,
            e.getMessage()));
      }
    });

    final JSONObject wrappedPolicy = new JSONObject();
    wrappedPolicy.put("metadata_policy", result.metadataPolicy());
    ctx.put(ContextKey.METADATA_POLICY, wrappedPolicy);
    ctx.put(ContextKey.SUBORDINATE_METADATA, new JSONObject(result.metadata()));

    final boolean invalidPolicy = problems.size() > result.failures().size();
    return new Evaluation(result, problems,
        invalidPolicy || (!result.failures().isEmpty()
            && template.onFailureOrFail() == MetadataPolicyTemplate.OnFailure.FAIL), template);
  }

  /**
   * Reads the template a flow configures for this step, the default template when none is set.
   *
   * @param configured the {@value #TEMPLATE} configuration value, may be {@code null}
   * @return the template
   * @throws IllegalStateException if the template cannot be read
   */
  public MetadataPolicyTemplate template(final String configured) {
    return this.parse(configured == null || configured.isBlank() ? this.presetTemplate : configured);
  }

  private String templateJson(final StepConfig config) {
    final Object configured = config.getValue(TEMPLATE);
    return configured == null || configured.toString().isBlank() ? this.presetTemplate : configured.toString();
  }

  private MetadataPolicyTemplate parse(final String json) {
    try {
      return this.jsonMapper.readerFor(MetadataPolicyTemplate.class)
          .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .readValue(json);
    }
    catch (final JacksonException e) {
      throw new IllegalStateException("The metadata policy template cannot be read: " + e.getOriginalMessage(), e);
    }
  }

  @Override
  public List<String> validateConfiguration(final Map<String, String> values) {
    final String json = values.get(TEMPLATE);
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return this.parse(json).problems().stream().map(problem -> "template: " + problem).toList();
    }
    catch (final IllegalStateException e) {
      return List.of(e.getMessage());
    }
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("FCF26DE1-93BF-4B80-A5E5-F4C88BDFFFEA");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(
        new StepConfigurationValue(MANUAL_REVIEW, StepConfigurationValue.DATA_TYPE.BOOLEAN,
            "If true, registration requires manual approval before proceeding", "false"),
        ReviewableStep.manualReviewForUpdateValue(),
        new StepConfigurationValue(TEMPLATE, StepConfigurationValue.DATA_TYPE.JSON,
            "Rules per entity type for the metadata and metadata_policy claims; the default template when not set",
            this.presetTemplate));
  }

  /**
   * The outcome of applying the template once.
   *
   * @param result the claims and check outcomes
   * @param problems every failed check and policy error, as messages
   * @param failed whether the step must fail
   * @param template the template applied
   */
  public record Evaluation(MetadataPolicyGenerator.Result result, List<String> problems, boolean failed,
      MetadataPolicyTemplate template) {

    private List<StepIssue> issues(final Severity severity) {
      return this.problems.stream()
          .map(problem -> new StepIssue("MetadataPolicyCreationStep", problem, severity))
          .toList();
    }
  }
}
