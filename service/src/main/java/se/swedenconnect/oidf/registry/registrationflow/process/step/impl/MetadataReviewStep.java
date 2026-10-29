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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import net.minidev.json.JSONObject;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.infrastructure.auth.domain.OrganizationRecord;
import se.swedenconnect.oidf.registry.registrationflow.metadata.FetchedResource;
import se.swedenconnect.oidf.registry.registrationflow.metadata.PinnedValues;
import se.swedenconnect.oidf.registry.registrationflow.metadata.ResourceFetcher;
import se.swedenconnect.oidf.registry.registrationflow.process.ContextKey;
import se.swedenconnect.oidf.registry.registrationflow.process.ProcessContext;
import se.swedenconnect.oidf.registry.registrationflow.process.review.MetadataDiff;
import se.swedenconnect.oidf.registry.registrationflow.process.review.PreviewSection;
import se.swedenconnect.oidf.registry.registrationflow.process.review.ReviewableStep;
import se.swedenconnect.oidf.registry.registrationflow.process.review.StepPreview;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfig;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepConfigurationValue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepIssue;
import se.swedenconnect.oidf.registry.registrationflow.process.step.StepResult;
import se.swedenconnect.oidf.registry.registrationflow.process.step.Severity;
import se.swedenconnect.oidf.registry.subordinate.dto.SubordinateDto;
import se.swedenconnect.oidf.registry.subordinate.service.SubordinateService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Shows an operator the metadata an entity registers with, whatever step loaded it: the logo as an image, the names per
 * language, the links, what changed since the hosted entity was last updated, for an entity already in the federation
 * how the values its Subordinate Statement pins differ from the Entity Configuration, and the full metadata. Always
 * pauses for review; a flow that should not check metadata leaves the step out.
 *
 * <p>Must come after the step that loads the metadata ({@code LoadEntityConfigurationStep} or
 * {@code HostedEntityRegistrationStep}). Logos are fetched in {@code buildContext} under the rules of
 * {@link ResourceFetcher}, and kept so that later steps check the same copy.</p>
 *
 * @author Felix Hellman
 */
@Component
public class MetadataReviewStep extends NoConfigStepAdapter implements ReviewableStep {

  /** Parameters shown as images. */
  static final List<String> IMAGE_PARAMETERS = List.of("logo_uri");

  private final ResourceFetcher resourceFetcher;
  private final SubordinateService subordinateService;

  /**
   * Constructor.
   *
   * @param resourceFetcher fetcher for images the metadata points to
   * @param subordinateService service reading the Subordinate Statement already published for the entity
   */
  public MetadataReviewStep(final ResourceFetcher resourceFetcher, final SubordinateService subordinateService) {
    this.resourceFetcher = resourceFetcher;
    this.subordinateService = subordinateService;
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public String getDescription() {
    return "Shows the operator the entity's metadata (logo, names, links and changes) and always waits for approval. "
        + "Place it after the step that loads the metadata.";
  }

  @Override
  public boolean requiresManualReview(final StepConfig config) {
    return true;
  }

  @Override
  public StepResult buildContext(final ProcessContext ctx, final StepConfig config) {
    final JSONObject metadata = ctx.<JSONObject>get(ContextKey.ENTITY_CONFIGURATION_METADATA).orElse(null);
    if (metadata == null) {
      return StepResult.failure("No metadata to review", List.of(StepIssue.technical("MetadataReviewStep",
          "Metadata Review must come after the step that loads the entity's metadata", Severity.ERROR)));
    }
    final OrganizationRecord organization = ctx.getRequired(ContextKey.ORG);
    final String selfHostedEntityId = ctx.get(ContextKey.REQUEST_METADATA).isPresent()
        ? null
        : ctx.<String>get(ContextKey.ENTITY_ID).orElse(null);
    final JSONObject fetched = ctx.<JSONObject>get(ContextKey.FETCHED_RESOURCES).orElseGet(JSONObject::new);
    for (final String url : imageUrls(metadata)) {
      if (!fetched.containsKey(url)) {
        fetched.put(url, this.resourceFetcher.fetchImage(url, organization, selfHostedEntityId).toMap());
      }
    }
    if (!fetched.isEmpty()) {
      ctx.put(ContextKey.FETCHED_RESOURCES, fetched);
    }
    return StepResult.success("Metadata loaded for review");
  }

  @Override
  public StepResult execute(final ProcessContext ctx, final StepConfig config) {
    return StepResult.success("Metadata approved");
  }

  @Override
  public StepPreview preview(final ProcessContext ctx, final StepConfig config) {
    final JSONObject metadata = ctx.getRequired(ContextKey.ENTITY_CONFIGURATION_METADATA, JSONObject.class);
    final boolean hosted = ctx.get(ContextKey.REQUEST_METADATA).isPresent();
    final JSONObject fetched = ctx.<JSONObject>get(ContextKey.FETCHED_RESOURCES).orElseGet(JSONObject::new);
    final String entityId = ctx.getRequired(ContextKey.ENTITY_ID);

    final StepPreview.Builder preview = StepPreview.builder()
        .add(PreviewSection.fields("Source", List.of(
            new PreviewSection.Field("Metadata from", hosted
                ? "Join request (hosted entity)"
                : "Entity Configuration of " + entityId),
            new PreviewSection.Field("Entity types", String.join(", ", metadata.keySet())))));

    for (final String url : imageUrls(metadata)) {
      final FetchedResource image = fetched.get(url) instanceof final Map<?, ?> known
          ? FetchedResource.fromMap(known)
          : FetchedResource.notFetched(url, "Not fetched");
      preview.add(PreviewSection.image("Logo", new PreviewSection.Image(url, image.contentType(), image.dataUri(),
          image.isImage()
              ? "%s, %d KB".formatted(image.contentType(), Math.max(1, image.size() / 1024))
              : image.error())));
    }

    final List<PreviewSection.Field> names = new ArrayList<>();
    final List<String> links = new ArrayList<>();
    metadata.forEach((entityType, parameters) -> {
      if (parameters instanceof final Map<?, ?> map) {
        map.forEach((key, value) -> {
          final String name = String.valueOf(key);
          final String base = name.split("#")[0];
          if (base.endsWith("_name")) {
            names.add(new PreviewSection.Field(name, String.valueOf(value)));
          }
          else if (base.endsWith("_uri") && !IMAGE_PARAMETERS.contains(base) && value instanceof String) {
            links.add(name + "  " + value);
          }
        });
      }
    });
    preview.add(names.isEmpty() ? null : PreviewSection.fields("Names", names));
    preview.add(links.isEmpty() ? null : PreviewSection.list("Links", links));

    ctx.<JSONObject>get(ContextKey.HOSTED_PREVIOUS_METADATA).ifPresent(previous -> preview.add(
        PreviewSection.diff("Changes to the hosted metadata", MetadataDiff.compare(previous, metadata))));

    if (!hosted) {
      this.published(ctx).ifPresent(published -> preview.add(PreviewSection.diff(
          "Changes to the values published in the federation", pinnedChanges(published, metadata, ctx))));
    }

    return preview.add(PreviewSection.json("Full metadata", metadata)).build();
  }

  /** The Subordinate Statement already published for the entity under this Intermediate, if any. */
  private Optional<SubordinateDto> published(final ProcessContext ctx) {
    final Optional<UUID> taImId = ctx.get(ContextKey.TAIM_ID);
    return taImId.flatMap(id -> this.subordinateService.getByEntityidentifierAndTaIm(
        ctx.getRequired(ContextKey.ENTITY_ID), id));
  }

  /**
   * Compares the values and keys the published Subordinate Statement pins with the same parameters and keys in the
   * entity's Entity Configuration. The federation keeps the published values until the registration is approved.
   */
  @SuppressWarnings("unchecked")
  private static List<PreviewSection.DiffEntry> pinnedChanges(final SubordinateDto published,
      final JSONObject metadata, final ProcessContext ctx) {
    final Map<String, Object> before = new LinkedHashMap<>();
    final Map<String, Object> after = new LinkedHashMap<>();
    for (final PinnedValues.Parameter parameter : PinnedValues.parameters(published.getMetadataPolicy(),
        published.getMetadata())) {
      ((Map<String, Object>) before.computeIfAbsent(parameter.entityType(), type -> new LinkedHashMap<>()))
          .put(parameter.parameter(), parameter.value());
      final Object current = metadata.get(parameter.entityType()) instanceof final Map<?, ?> declared
          ? declared.get(parameter.parameter())
          : null;
      ((Map<String, Object>) after.computeIfAbsent(parameter.entityType(), type -> new LinkedHashMap<>()))
          .put(parameter.parameter(), current);
    }
    before.put("jwks", keyIds(published.getJwks()));
    after.put("jwks", ctx.<JWKSet>get(ContextKey.ENTITY_CONFIGURATION_JWKS)
        .map(jwks -> jwks.getKeys().stream().map(JWK::getKeyID).filter(Objects::nonNull).sorted().toList())
        .orElse(List.of()));
    return MetadataDiff.compare(before, after);
  }

  private static List<String> keyIds(final Map<String, Object> jwks) {
    if (jwks == null || !(jwks.get("keys") instanceof final List<?> keys)) {
      return List.of();
    }
    return keys.stream()
        .map(key -> key instanceof final Map<?, ?> map ? map.get("kid") : null)
        .filter(Objects::nonNull)
        .map(String::valueOf)
        .sorted()
        .toList();
  }

  private static List<String> imageUrls(final JSONObject metadata) {
    final List<String> urls = new ArrayList<>();
    metadata.values().forEach(parameters -> {
      if (parameters instanceof final Map<?, ?> map) {
        IMAGE_PARAMETERS.forEach(parameter -> {
          if (map.get(parameter) instanceof final String url && !url.isBlank() && !urls.contains(url)) {
            urls.add(url);
          }
        });
      }
    });
    return urls;
  }

  @Override
  public UUID getStepId() {
    return UUID.fromString("5D2C8E14-7A3B-4C9F-9E61-2B8F4A7D0C35");
  }

  @Override
  public List<StepConfigurationValue> getStepConfigurationValues() {
    return List.of(ReviewableStep.manualReviewForUpdateValue());
  }
}
