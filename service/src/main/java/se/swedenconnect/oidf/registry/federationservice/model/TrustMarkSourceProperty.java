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
package se.swedenconnect.oidf.registry.federationservice.model;

import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

import java.io.Serializable;

/**
 * Data class for trust mark source.
 *
 * @param issuer of the trust mark
 * @param trustMarkType of the trust mark
 * @author Felix Hellman
 */
public record TrustMarkSourceProperty(@SerializedName("issuer") EntityID issuer,
    @SerializedName("trust-mark-type") String trustMarkType) implements Serializable {
}
