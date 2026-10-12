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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * One section of a {@link StepPreview}. Only the content field that matches {@link #type()} is set; the others are
 * {@code null} and left out of the JSON.
 *
 * @param type how the front end renders the section
 * @param label heading shown above the section
 * @param fields name and value pairs, for {@link Type#FIELDS}
 * @param checks check outcomes, for {@link Type#CHECKS}
 * @param json a JSON object or array shown as code, for {@link Type#JSON}
 * @param items plain list entries, for {@link Type#LIST}
 * @param text free text, for {@link Type#TEXT}
 * @param image an image reference, for {@link Type#IMAGE}
 * @param diff changes between two versions, for {@link Type#DIFF}
 * @author Felix Hellman
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "PreviewSection", description = "One typed section of a step preview")
public record PreviewSection(
    @Schema(description = "How the section is rendered") Type type,
    @Schema(description = "Heading shown above the section") String label,
    @Schema(description = "Name and value pairs, for FIELDS") List<Field> fields,
    @Schema(description = "Check outcomes, for CHECKS") List<Check> checks,
    @Schema(description = "JSON object or array shown as code, for JSON") Object json,
    @Schema(description = "Plain list entries, for LIST") List<String> items,
    @Schema(description = "Free text, for TEXT") String text,
    @Schema(description = "Image reference, for IMAGE") Image image,
    @Schema(description = "Changes between two versions, for DIFF") List<DiffEntry> diff) {

  /**
   * How a section is rendered.
   *
   * @author Felix Hellman
   */
  public enum Type {
    /** Name and value pairs. */
    FIELDS,
    /** Check outcomes with a status each. */
    CHECKS,
    /** A JSON document shown as code. */
    JSON,
    /** A plain list. */
    LIST,
    /** Free text. */
    TEXT,
    /** An image, such as a logo. */
    IMAGE,
    /** Added, changed and removed values between two versions. */
    DIFF
  }

  /**
   * Outcome of one check.
   *
   * @author Felix Hellman
   */
  public enum CheckStatus {
    /** The check passed. */
    OK,
    /** The check failed. */
    FAIL,
    /** Information about something the registry did, such as fetching a resource. */
    INFO
  }

  /**
   * Kind of change in a {@link DiffEntry}.
   *
   * @author Felix Hellman
   */
  public enum ChangeType {
    /** The value is new. */
    ADDED,
    /** The value changed. */
    CHANGED,
    /** The value was removed. */
    REMOVED
  }

  /**
   * A name and value pair.
   *
   * @param name the name, such as a metadata parameter
   * @param value the value as shown to the operator
   * @author Felix Hellman
   */
  @Schema(name = "PreviewField")
  public record Field(String name, String value) {
  }

  /**
   * The outcome of one check.
   *
   * @param status the outcome
   * @param message what was checked, and why it failed if it did
   * @author Felix Hellman
   */
  @Schema(name = "PreviewCheck")
  public record Check(CheckStatus status, String message) {
  }

  /**
   * An image to show the operator.
   *
   * @param url where the image was published
   * @param contentType the media type, if known
   * @param dataUri the copy the registry fetched and checked, as a {@code data:} URI; {@code null} if not fetched
   * @param note extra information, such as size or why the image was not fetched
   * @author Felix Hellman
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(name = "PreviewImage")
  public record Image(String url, String contentType, String dataUri, String note) {
  }

  /**
   * One change between two versions.
   *
   * @param changeType the kind of change
   * @param key the changed value's path, such as {@code openid_relying_party.logo_uri}
   * @param before the old value, {@code null} for {@link ChangeType#ADDED}
   * @param after the new value, {@code null} for {@link ChangeType#REMOVED}
   * @author Felix Hellman
   */
  @Schema(name = "PreviewDiffEntry")
  public record DiffEntry(ChangeType changeType, String key, String before, String after) {
  }

  /**
   * Creates a {@link Type#FIELDS} section.
   *
   * @param label the heading
   * @param fields the name and value pairs
   * @return the section
   */
  public static PreviewSection fields(final String label, final List<Field> fields) {
    return new PreviewSection(Type.FIELDS, label, List.copyOf(fields), null, null, null, null, null, null);
  }

  /**
   * Creates a {@link Type#CHECKS} section.
   *
   * @param label the heading
   * @param checks the check outcomes
   * @return the section
   */
  public static PreviewSection checks(final String label, final List<Check> checks) {
    return new PreviewSection(Type.CHECKS, label, null, List.copyOf(checks), null, null, null, null, null);
  }

  /**
   * Creates a {@link Type#JSON} section.
   *
   * @param label the heading
   * @param json a JSON object or array, as maps and lists
   * @return the section
   */
  public static PreviewSection json(final String label, final Object json) {
    return new PreviewSection(Type.JSON, label, null, null, json, null, null, null, null);
  }

  /**
   * Creates a {@link Type#LIST} section.
   *
   * @param label the heading
   * @param items the entries
   * @return the section
   */
  public static PreviewSection list(final String label, final List<String> items) {
    return new PreviewSection(Type.LIST, label, null, null, null, List.copyOf(items), null, null, null);
  }

  /**
   * Creates a {@link Type#TEXT} section.
   *
   * @param label the heading
   * @param text the text
   * @return the section
   */
  public static PreviewSection text(final String label, final String text) {
    return new PreviewSection(Type.TEXT, label, null, null, null, null, text, null, null);
  }

  /**
   * Creates an {@link Type#IMAGE} section.
   *
   * @param label the heading
   * @param image the image
   * @return the section
   */
  public static PreviewSection image(final String label, final Image image) {
    return new PreviewSection(Type.IMAGE, label, null, null, null, null, null, image, null);
  }

  /**
   * Creates a {@link Type#DIFF} section.
   *
   * @param label the heading
   * @param diff the changes
   * @return the section
   */
  public static PreviewSection diff(final String label, final List<DiffEntry> diff) {
    return new PreviewSection(Type.DIFF, label, null, null, null, null, null, null, List.copyOf(diff));
  }
}
