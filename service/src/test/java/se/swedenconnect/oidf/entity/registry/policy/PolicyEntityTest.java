/*
 * Copyright 2025 Sweden Connect
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
package se.swedenconnect.oidf.entity.registry.policy;

import org.junit.jupiter.api.Test;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the PolicyDao class.
 * <p>
 * This class contains test methods to verify the functionality of setters, getters,
 * the default constructor, and the toString method of the PolicyDao class.
 *
 * @author David Goldring
 */
class PolicyEntityTest {

  private final String policy = """
      {
        "openid_relying_party" : {
          "grant_types" : {
            "subset_of" : [ "authorization_code" ]
          },
          "token_endpoint_auth_method" : {
            "superset_of" : [ "private_key_jwt" ],
            "essential" : true
          },
          "response_types" : {
            "subset_of" : [ "code" ]
          }
        }
      }
      """;

  /**
   * Verifies the functionality of the getters and setters of the PolicyDao class.
   */
  @Test
  public void testEntitySettersAndGetters() {
    PolicyEntity policyEntity = new PolicyEntity();

    policyEntity.setId(1L);
    policyEntity.setName("openid_relying_party");
    policyEntity.setPolicy(this.policy);

    assertThat(policyEntity.getId()).isEqualTo(1L);
    assertThat(policyEntity.getName()).isEqualTo("openid_relying_party");
    assertThat(policyEntity.getPolicy()).isEqualTo(this.policy);
  }

  /**
   * Test the default constructor of the PolicyDao class.
   */
  @Test
  public void testEntityDefaultConstructor() {
    PolicyEntity policyEntity = new PolicyEntity();

    assertThat(policyEntity.getId()).isEqualTo(0L);
    assertThat(policyEntity.getName()).isNull();
    assertThat(policyEntity.getPolicy()).isNull();
  }

}