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
package se.swedenconnect.oidf.entity.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            "issuer": "%s",
            "subject": "%s",
            "entity_record_id":"%s",
            "policy_record_id": "3ea2aace-18b3-439e-88b1-61d232f20fe9",
            "hosted_record": {
              "authority_hints": [
                "https://example.com/hint1",
                "https://example.com/hint2"
              ],
              "trust_mark_sources": [
                {
                  "trust_mark_id": "https://trust.example.com/tm1",
                  "issuer": "https://trust.example.com/issuer1"
                },
                {
                  "trust_mark_id": "https://trust.example.com/tm2",
                  "issuer": "https://trust.example.com/issuer2"
                }
              ]
            },
            "jwks": {
              "keys": [
                {
                  "kty": "RSA",
                  "kid": "key1",
                  "use": "sig",
                  "n": "v1hGQlmTWwoA8V3yHdF-...",
                  "e": "AQAB"
                },
                {
                  "kty": "RSA",
                  "kid": "key2",
                  "use": "enc",
                  "n": "w2Hfg34VWQowQAx3HtP-...",
                  "e": "AQAB"
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
    public static EntityRecord createDefaultEntity(String subject) {
        return createDefaultEntity(ISSUER_1,subject);
    }

    /**
     * Getting a test json
     * @param issuer Issuer id to set in json
     * @param subject Subject id to set in json
     * @return Json
     */
    public static String createDefaultJsonEntity(String issuer,String subject) {
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
     * Creating a full EntityObject
     * @param issuer Issuer set in entityobject
     * @param subject Subject set in entityobject
     * @return Entity object with selected issuer and subject
     */
    public static EntityRecord createDefaultEntity(String issuer, String subject) {
        try {
            return mapper.readValue(
                entityJsonData.formatted(issuer, subject,UUID.randomUUID().toString()), EntityRecord.class);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}