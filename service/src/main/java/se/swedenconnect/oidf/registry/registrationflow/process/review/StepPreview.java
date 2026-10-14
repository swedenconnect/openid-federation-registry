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
package se.swedenconnect.oidf.registry.registrationflow.process.review;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * What a paused step shows the operator: an ordered list of typed sections. The front end renders each section by
 * its type, so a new step needs no front-end change to be reviewable.
 *
 * @param sections the sections, in display order
 * @author Felix Hellman
 */
@Schema(name = "StepPreview", description = "What a step paused for manual review shows the operator")
public record StepPreview(
    @Schema(description = "Sections in display order")
    List<PreviewSection> sections) {

  /**
   * Creates a preview, copying the sections.
   *
   * @param sections the sections, in display order
   */
  public StepPreview {
    sections = sections == null ? List.of() : List.copyOf(sections);
  }

  /**
   * Creates a preview from the given sections.
   *
   * @param sections the sections, in display order
   * @return the preview
   */
  public static StepPreview of(final PreviewSection... sections) {
    return new StepPreview(List.of(sections));
  }

  /**
   * Creates a preview telling the operator that the step could not describe itself. Used when building the preview
   * fails, so that a broken preview never blocks the review.
   *
   * @param reason why the preview is missing
   * @return the preview
   */
  public static StepPreview unavailable(final String reason) {
    return of(PreviewSection.text("Preview unavailable", reason));
  }

  /**
   * Returns a builder for adding sections one by one, skipping those a step has nothing to show for.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Collects sections for a {@link StepPreview}.
   *
   * @author Felix Hellman
   */
  public static final class Builder {

    private final List<PreviewSection> sections = new ArrayList<>();

    private Builder() {
    }

    /**
     * Adds a section.
     *
     * @param section the section, ignored if {@code null}
     * @return this builder
     */
    public Builder add(final PreviewSection section) {
      if (section != null) {
        this.sections.add(section);
      }
      return this;
    }

    /**
     * Adds sections in order.
     *
     * @param sections the sections; {@code null} entries are ignored
     * @return this builder
     */
    public Builder addAll(final List<PreviewSection> sections) {
      sections.forEach(this::add);
      return this;
    }

    /**
     * Builds the preview.
     *
     * @return the preview
     */
    public StepPreview build() {
      return new StepPreview(this.sections);
    }
  }
}
