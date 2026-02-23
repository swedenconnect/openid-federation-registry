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
package se.swedenconnect.oidf.registry.federation.serde;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.ExclusionStrategy;
import com.nimbusds.jose.shaded.gson.FieldAttributes;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import se.swedenconnect.oidf.registry.federation.model.EntityRecord;
import se.swedenconnect.oidf.registry.federation.model.ModuleRecord;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Parses and loads json from registry.
 *
 * @author Felix Hellman
 */
public class JsonRegistryLoader {
  private final Gson GSON;

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
   * @param moduleRecord
   * @return json string
   */
  public String toJson(final ModuleRecord moduleRecord) {
    return this.GSON.getAdapter(ModuleRecord.class).toJson(moduleRecord);
  }

  /**
   * @param entityRecords
   * @return json string
   */
  public String toJson(final List<EntityRecord> entityRecords) {
    return this.GSON.getAdapter(new TypeToken<List<EntityRecord>>() {}).toJson(entityRecords);
  }
}

