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

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Objects;

/**
 * Implements {@link JsonDeserializer} and {@link JsonSerializer} for JWK.
 *
 * @author Felix Hellman
 */
@NoArgsConstructor
@AllArgsConstructor
public class JWKSSerializer implements JsonDeserializer<JWKSet>, JsonSerializer<JWKSet> {

  @Nullable
  private JsonDeserializer<JWKSet> referenceDeserializer;

  @Nullable
  private JsonSerializer<JWKSet> referenceSerializer;

  @Override
  public JWKSet deserialize(
      final JsonElement jsonElement,
      final Type type,
      final JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {
    try {
      if (jsonElement.isJsonObject()) {
        final String asString = jsonElement.getAsJsonObject().toString();
        return JWKSet.parse(asString);
      }
      else if (jsonElement.isJsonArray()) {
        final String asString = jsonElement.getAsJsonArray().toString();
        return JWKSet.parse(asString);
      }
      else {
        if (Objects.nonNull(this.referenceDeserializer)) {
          return this.referenceDeserializer.deserialize(jsonElement, type, jsonDeserializationContext);
        }
        throw new IllegalArgumentException("Failed to parse string type value to JWKS");
      }
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JsonElement serialize(
      final JWKSet jwks,
      final Type type,
      final JsonSerializationContext jsonSerializationContext) {
    if (Objects.nonNull(this.referenceSerializer)) {
      this.referenceSerializer.serialize(jwks, type, jsonSerializationContext);
    }
    return JsonParser.parseString(jwks.toString(true));
  }
}
