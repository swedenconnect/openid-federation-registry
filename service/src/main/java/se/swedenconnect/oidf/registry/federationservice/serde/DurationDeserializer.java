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

import com.nimbusds.jose.shaded.gson.JsonDeserializationContext;
import com.nimbusds.jose.shaded.gson.JsonDeserializer;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParseException;
import com.nimbusds.jose.shaded.gson.JsonPrimitive;
import com.nimbusds.jose.shaded.gson.JsonSerializationContext;
import com.nimbusds.jose.shaded.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Duration;

/**
 * Serializer for duration.
 *
 * @author Felix Hellman
 */
public class DurationDeserializer implements JsonDeserializer<Duration>, JsonSerializer<Duration> {
  @Override
  public Duration deserialize(
      final JsonElement jsonElement, final Type type, final JsonDeserializationContext jsonDeserializationContext)
      throws JsonParseException {
    return Duration.parse(jsonElement.getAsString());
  }

  @Override
  public JsonElement serialize(
      final Duration duration, final Type type, final JsonSerializationContext jsonSerializationContext) {
    return new JsonPrimitive(duration.toString());
  }
}
