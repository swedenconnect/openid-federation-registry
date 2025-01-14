/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */
package se.swedenconnect.oidf.entity.registry.fixture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.EntityEntity;
import se.swedenconnect.oidf.registry.api.model.EntityRecord;

import java.util.UUID;

/**
 * Factory class for creating instances of {@link EntityRecord}, from a json set
 *
 * @author David Goldring
 */
public class EntityFactory {


    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * URL representing the first subject in the system.
     * This constant is used as a default subject when creating entities.
     */
    public final static String SUBJECT_1 = "https://example.com/subject/1";

    public final static String ISSUER_1 = "https://example.com/issuer/1";

    /**
     * The constant URL for a second subject.
     */
    public final static String SUBJECT_2 = "https://example.com/subject/2";

    /**
     * A constant representing a third subject.
     */
    public final static String SUBJECT_3 = "https://example.com/subject/3";

    /**
     * The default subject used for creating instances of {@link EntityRecord}.
     * This constant is typically used when no specific subject is provided.
     */
    public final static String SUBJECT_DEFAULT = SUBJECT_1;


    private static final String entityJsonData = """
        {
           "entity_record_id": "123e4567-e89b-12d3-a456-426614174000",
           "issuer": "%s",
           "subject": "%s",
           "policy_record_id": "%s",
           "override_configuration_location": "https://config.example.com/metadata",
           "hosted_record": {
             "metadata": {
               "key1": "value1",
               "key2": "value2"
             },
             "authority_hints": [
               "https://authority1.example.com",
               "https://authority2.example.com"
             ],
             "trust_mark_sources": [
               {
                 "trust_mark_id": "tm12345",
                 "issuer": "https://trustmark.example.com"
               },
               {
                 "trust_mark_id": null,
                 "issuer": null
               }
             ]
           },
           "jwks": {
             "keys": [
               {
                 "kty": "RSA",
                 "kid": "key1",
                 "use": "sig",
                 "n": "random_base64url_value",
                 "e": "AQAB"
               },
               {
                 "kty": "EC",
                 "kid": "key2",
                 "use": "enc",
                 "crv": "P-256",
                 "x": "random_base64url_value",
                 "y": "random_base64url_value"
               }
             ]
           }
         }
        """;



    /**
     * Creates an instance of {@link EntityRecord} with default values.
     * Uses JwksSource by default and does not include a Hosted object.
     *
     * @return an instance of {@link EntityRecord}
     */
    public static EntityRecord createDefaultEntity() {
        return createDefaultEntity(SUBJECT_DEFAULT);
    }

    /**
     * Creates a default instance of {@link EntityRecord} with given subject.
     * Uses a default JwksSource and predefined URL and policy.
     *
     * @param subject the subject of the entity
     * @return an instance of {@link EntityRecord}
     */
    public static EntityRecord createDefaultEntity(final String subject) {
        return createDefaultEntity(ISSUER_1,subject);
    }

    /**
     * Getting a test json
     * @param issuer Issuer id to set in json
     * @param subject Subject id to set in json
     * @return Json
     */
    public static String createDefaultJsonEntity(final String issuer,final String subject) {
        return entityJsonData.formatted(issuer, subject, UUID.randomUUID().toString());
    }

    /**
     * Getting a test json
     * @return Json
     */
    public static String createDefaultJsonEntity() {
        return entityJsonData.formatted(ISSUER_1, SUBJECT_1,UUID.randomUUID().toString());
    }

    /**
     * Creating a full EntityObject with random policyid and entityrecordid
     * @param issuer Issuer set in entityobject
     * @param subject Subject set in entityobject
     * @return Entity object with selected issuer and subject
     */
    public static EntityRecord createDefaultEntity(final String issuer, final String subject) {
        try {
            EntityRecord entityRecord =  mapper.readValue(
                entityJsonData, EntityRecord.class);
            entityRecord.setEntityRecordId(UUID.randomUUID().toString());
            entityRecord.setPolicyRecordId(UUID.randomUUID().toString());
            entityRecord.setIssuer(issuer);
            entityRecord.setSubject(subject);
            return entityRecord;
        }
        catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Merges the data from the provided {@link EntityRecord} into an existing {@link EntityEntity} object.
     * Updates the issuer, subject, and entity fields of the {@link EntityEntity}.
     * Sets the externalId of the {@link EntityEntity} if it is null, using the entityRecordId from {@link EntityRecord}.
     * Throws an exception in case of JSON processing errors during mapping.
     *
     * @param record the {@link EntityRecord} containing the data to be merged
     * @param entity the {@link EntityEntity} to be updated with data from the given {@link EntityRecord}
     * @return the updated {@link EntityEntity} object with the merged data
     * @throws ResponseStatusException if a JSON processing error occurs during data mapping
     */
    public static EntityEntity mergeRecordIntoEntity(final EntityRecord record, final EntityEntity entity) {
        try {
            entity.setIssuer(record.getIssuer());
            entity.setSubject(record.getSubject());
            entity.setEntity(mapper.writeValueAsString(record));
            if (entity.getExternalId() == null) {
                entity.setExternalId(record.getEntityRecordId());
            }
            return entity;
        }
        catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to map record to entity", e);
        }
    }

}