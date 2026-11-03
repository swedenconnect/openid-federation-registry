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
package se.swedenconnect.oidf.registry.federationservice.serde;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.registry.federationservice.model.EntityRecord;
import se.swedenconnect.oidf.registry.federationservice.model.ModuleRecord;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses and loads json from registry.
 *
 * @author Felix Hellman
 */
public class JsonRegistryLoader {
  private static final String TRUST_MARK_ISSUERS = "trust-mark-issuers";
  private static final String POLICY = "policy";

  private final Gson GSON;

  /** Keeps null values, so that {@code value: null} in a metadata policy reaches the federation service. */
  private final Gson moduleGson;

  /**
   * Constructor.
   */
  public JsonRegistryLoader() {
    this.GSON = new GsonBuilder()
        .addDeserializationExclusionStrategy(new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
            return false;
          }

          @Override
          public boolean shouldSkipClass(final Class<?> aClass) {
            return false;
          }
        })
        .registerTypeAdapter(Duration.class, new DurationDeserializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer())
        .registerTypeAdapter(EntityID.class, new EntityIdentifierDeserializer())
        .registerTypeAdapter(JWK.class, new JWKSerializer())
        .registerTypeAdapter(JWKSet.class, new JWKSSerializer())
        .create();
    this.moduleGson = this.GSON.newBuilder().serializeNulls().create();
  }

  /**
   * Parse EntityRecords from json
   *
   * @param json
   * @return list of entities
   */
  public List<EntityRecord> parseEntityRecord(final String json) {
    try {
      final TypeAdapter<List<EntityRecord>> adapter = this.GSON.getAdapter(new TypeToken<>() {
      });
      return adapter.fromJson(json);
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parse module record from json
   *
   * @param json
   * @return module record
   */
  public ModuleRecord parseModuleJson(final String json) {
    try {
      final TypeAdapter<ModuleRecord> adapter = this.GSON.getAdapter(new TypeToken<>() {
      });
      return adapter.fromJson(json);
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Serializes the module record. Values without content (null, empty objects and empty arrays) are left out
   * everywhere below the three top level lists, which are always present. The empty lists of issuers inside a
   * trust-mark-issuers object are kept, since they have a meaning. The metadata policy of a policy record is kept as
   * it is, since {@code "value": null} means that the parameter must be absent.
   *
   * @param moduleRecord
   * @return json string
   */
  public String toJson(final ModuleRecord moduleRecord) {
    final JsonObject root = this.moduleGson.toJsonTree(moduleRecord, ModuleRecord.class).getAsJsonObject();
    for (final Map.Entry<String, JsonElement> entry : root.entrySet()) {
      if (entry.getValue().isJsonArray()) {
        final JsonArray pruned = new JsonArray();
        entry.getValue().getAsJsonArray().forEach(element -> pruned.add(prune(element, false)));
        entry.setValue(pruned);
      }
    }
    return this.moduleGson.toJson(root);
  }

  /**
   * Removes null values, empty objects and empty arrays from the given element, recursively.
   *
   * @param element to prune
   * @param policyRecord whether the element is a policy record, whose policy is kept as it is
   * @return the pruned element
   */
  private static JsonElement prune(final JsonElement element, final boolean policyRecord) {
    if (element.isJsonObject()) {
      final JsonObject object = element.getAsJsonObject();
      for (final String name : new ArrayList<>(object.keySet())) {
        final JsonElement value = object.get(name);
        final boolean policy = POLICY.equals(name) && value.isJsonObject();
        // An empty list of issuers means that anyone may issue the trust mark type, so it has to be kept.
        final JsonElement pruned = (TRUST_MARK_ISSUERS.equals(name) && value.isJsonObject())
            || (policyRecord && policy)
            ? value
            : prune(value, policy);
        if (isEmpty(pruned)) {
          object.remove(name);
        }
        else {
          object.add(name, pruned);
        }
      }
    }
    else if (element.isJsonArray()) {
      final JsonArray array = element.getAsJsonArray();
      final List<JsonElement> kept = new ArrayList<>();
      array.forEach(item -> kept.add(prune(item, false)));
      kept.removeIf(JsonRegistryLoader::isEmpty);
      final JsonArray result = new JsonArray();
      kept.forEach(result::add);
      return result;
    }
    return element;
  }

  private static boolean isEmpty(final JsonElement element) {
    return element.isJsonNull()
        || (element.isJsonObject() && element.getAsJsonObject().size() == 0)
        || (element.isJsonArray() && element.getAsJsonArray().size() == 0);
  }

  /**
   * @param entityRecords
   * @return json string
   */
  public String toJson(final List<EntityRecord> entityRecords) {
    return this.GSON.getAdapter(new TypeToken<List<EntityRecord>>() {}).toJson(entityRecords);
  }
}

