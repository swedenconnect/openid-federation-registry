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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The outcome of fetching a resource an entity's metadata points to, such as its logo. A fetched image or JSON document
 * is kept as a {@code data:} URI so that the reviewer sees the copy that was checked, not whatever the URL serves later.
 *
 * @param url the URL that was fetched
 * @param contentType the media type the server gave, {@code null} if not fetched
 * @param size the size in bytes, {@code null} if not fetched
 * @param dataUri the content as a {@code data:} URI, {@code null} if not fetched or not an image or JSON document
 * @param error why the resource was not fetched or is not usable, {@code null} on success
 * @author Felix Hellman
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FetchedResource(
    @JsonProperty("url") String url,
    @JsonProperty("content_type") String contentType,
    @JsonProperty("size") Long size,
    @JsonProperty("data_uri") String dataUri,
    @JsonProperty("error") String error) {

  /**
   * Creates the outcome of a resource that was not fetched.
   *
   * @param url the URL
   * @param error why it was not fetched
   * @return the outcome
   */
  public static FetchedResource notFetched(final String url, final String error) {
    return new FetchedResource(url, null, null, null, error);
  }

  /**
   * Tells whether the resource was fetched and is an image.
   *
   * @return {@code true} for a fetched image
   */
  @JsonIgnore
  public boolean isImage() {
    return this.error == null && this.contentType != null && this.contentType.startsWith("image/");
  }

  /**
   * Converts this outcome to a map, the form it is kept in the pipeline context.
   *
   * @return the map
   */
  public Map<String, Object> toMap() {
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("url", this.url);
    putIfPresent(map, "content_type", this.contentType);
    putIfPresent(map, "size", this.size);
    putIfPresent(map, "data_uri", this.dataUri);
    putIfPresent(map, "error", this.error);
    return map;
  }

  /**
   * Reads an outcome back from the map made by {@link #toMap()}.
   *
   * @param map the map
   * @return the outcome
   */
  public static FetchedResource fromMap(final Map<?, ?> map) {
    final Object size = map.get("size");
    return new FetchedResource((String) map.get("url"), (String) map.get("content_type"),
        size instanceof final Number number ? number.longValue() : null, (String) map.get("data_uri"),
        (String) map.get("error"));
  }

  private static void putIfPresent(final Map<String, Object> map, final String key, final Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }
}
