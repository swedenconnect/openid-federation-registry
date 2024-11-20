/*
 * Copyright 2024 Sweden Connect.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.oidf.entity.util;

import se.swedenconnect.oidf.registry.api.model.Entity;
import se.swedenconnect.oidf.registry.api.model.JwkSource;
import se.swedenconnect.oidf.registry.api.model.Hosted;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating instances of {@link Entity}, {@link JwkSource},
 * {@link Hosted}, and {@link TrustMarkSource}.
 *
 * @author David Goldring
 */
public class EntityFactory {

    /**
     * URL representing the first subject in the system.
     * This constant is used as a default subject when creating entities.
     */
    public final static String SUBJECT_1 = "https://example.com/subject/1";

    /**
     * The constant URL for a second subject.
     */
    public final static String SUBJECT_2 = "https://example.com/subject/2";

    /**
     * A constant representing a third subject.
     */
    public final static String SUBJECT_3 = "https://example.com/subject/3";

    /**
     * The default subject used for creating instances of {@link Entity}.
     * This constant is typically used when no specific subject is provided.
     */
    public final static String SUBJECT_DEFAULT = SUBJECT_1;

    /**
     * Creates an instance of {@link Entity} with default values.
     * Uses {@link JwkSource} by default and does not include a {@link Hosted} object.
     *
     * @return an instance of {@link Entity}
     */
    public static Entity createDefaultEntity() {
        return createDefaultEntity(SUBJECT_DEFAULT);
    }

    /**
     * Creates a default instance of {@link Entity} with given subject.
     * Uses a default {@link JwkSource} and predefined URL and policy.
     *
     * @param subject the subject of the entity
     * @return an instance of {@link Entity}
     */
    public static Entity createDefaultEntity(String subject) {
        final List<JwkSource> jwkSources = new ArrayList<>();
        jwkSources.add(createJwkSource(
            null,
            null,
            "eyJrdHki.....HRBIn0="
        ));

        return createEntityWithJwkSource(
            subject,
            jwkSources,
            "https://example.com/subject/3/.well-known/openid-federation",
            "policy2.json",
            false
        );
    }

    /**
     * Creates an instance of {@link Entity} using a {@link Hosted} object.
     * {@link JwkSource} list will be set to null.
     *
     * @param subject      the subject of the entity
     * @param location     the location URL
     * @param policy       the policy file name
     * @param hosted       the hosted object
     * @param intermediate whether the entity is intermediate
     *
     * @return an instance of {@link Entity}
     */
    public static Entity createEntityWithHosted(final String subject, final String location, final String policy, final Hosted hosted, final boolean intermediate) {
        final Entity entity = new Entity();

        entity.setSubject(subject);
        entity.setJwk(null); // Ensuring JwkSource is null
        entity.setLocation(location);
        entity.setPolicy(policy);
        entity.setHosted(hosted);
        entity.setIntermediate(intermediate);

        return entity;
    }

    /**
     * Creates an instance of {@link Entity} using {@link JwkSource} list.
     * {@link Hosted} will be set to null.
     *
     * @param subject      the subject of the entity
     * @param jwk          the list of JWK sources
     * @param location     the location URL
     * @param policy       the policy file name
     * @param intermediate whether the entity is intermediate
     *
     * @return an instance of {@link Entity}
     */
    public static Entity createEntityWithJwkSource(final String subject, final List<JwkSource> jwk, final String location, final String policy, final boolean intermediate) {
        final Entity entity = new Entity();

        entity.setIssuer("http://tmi.digg.se");
        entity.setSubject(subject);
        entity.setJwk(jwk);
        entity.setLocation(location);
        entity.setPolicy(policy);
        entity.setHosted(null); // Ensuring Hosted is null
        entity.setIntermediate(intermediate);

        return entity;
    }

    /**
     * Creates an instance of {@link JwkSource} with specified values.
     *
     * @param kid          the key identifier
     * @param certLocation the location of the certificate file
     * @param base64jwk    the base64 encoded JWK json string
     *
     * @return an instance of {@link JwkSource}
     */
    public static JwkSource createJwkSource(final String kid, final String certLocation, final String base64jwk) {
        final JwkSource jwkSource = new JwkSource();

        jwkSource.setKid(kid);
        jwkSource.setCertLocation(certLocation);
        jwkSource.setBase64jwk(base64jwk);

        return jwkSource;
    }

    /**
     * Creates an instance of {@link Hosted} with specified values.
     *
     * @param metadata   the metadata file name
     * @param trustMarks the list of trust marks
     *
     * @return an instance of {@link Hosted}
     */
    public static Hosted createHosted(final String metadata, final List<TrustMarkSource> trustMarks) {
        final Hosted hosted = new Hosted();

        hosted.setMetadata(metadata);
        hosted.setTrustMarks(trustMarks);

        return hosted;
    }

    /**
     * Creates an instance of {@link TrustMarkSource} with specified values.
     *
     * @param trustMarkId the ID of the Trust Mark
     * @param issuer      the Entity Identifier of the Trust Mark issuer
     *
     * @return an instance of {@link TrustMarkSource}
     */
    public static TrustMarkSource createTrustMarkSource(final String trustMarkId, final String issuer) {
        final TrustMarkSource trustMarkSource = new TrustMarkSource();

        trustMarkSource.setTrustMarkId(trustMarkId);
        trustMarkSource.setIssuer(issuer);

        return trustMarkSource;
    }

    // Methods for creating pre-defined entities based on example data from
    // https://github.com/swedenconnect/openid-federation-node/blob/main/README.md

    /**
     * Creates an entity with specific Hosted object and one Trust Mark.
     * Subject: {@code "https://example.com/subject/1"}
     *
     * @return an instance of {@link Entity}
     */
    public static Entity createEntityWithSingleTrustMark() {
        final List<TrustMarkSource> trustMarkSources = new ArrayList<>();
        trustMarkSources.add(createTrustMarkSource(
            "https://example.com/trust-mark-id/2",
            "https://example.com/trust_mark"
        ));
        final Hosted hosted = createHosted("subj1metadata.json", trustMarkSources);

        return createEntityWithHosted(
            "https://example.com/subject/1",
            null,
            "policy1.json",
            hosted,
            false
        );
    }

    /**
     * Creates an entity with specific Hosted object and multiple Trust Marks.
     * Subject: {@code "https://example.com/subject/2"}
     *
     * @return an instance of {@link Entity}
     */
    public static Entity createEntityWithMultipleTrustMarks() {
        final List<TrustMarkSource> trustMarkSources = new ArrayList<>();
        trustMarkSources.add(createTrustMarkSource(
            "https://example.com/trust-mark-id/1",
            "https://example.com/trust_mark"
        ));
        trustMarkSources.add(createTrustMarkSource(
            "https://example.com/trust-mark-id/2",
            "https://example.com/trust_mark"
        ));
        final Hosted hosted = createHosted("subj2metadata.json", trustMarkSources);

        return createEntityWithHosted(
            SUBJECT_2,
            null,
            "policy1.json",
            hosted,
            false
        );
    }

    /**
     * Creates an entity with JwkSource and no Hosted object.
     * Subject: {@code "https://example.com/subject/3"}
     *
     * @return an instance of {@link Entity}
     */
    public static Entity createEntityWithJwkSourceOnly() {
      return createDefaultEntity();
    }
}