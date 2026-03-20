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

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * Record class of an individual policy.
 *
 * @author Felix Hellman
 */
@Getter
@Setter
public class PolicyRecord implements Serializable {

  /**
   * Constructor.
   * @param id of the policy
   * @param policy object of the policy
   */
  public PolicyRecord(final String id, final Map<String, Object> policy) {
    this.id = id;
    this.policy = policy;
  }

  /**
   * Default constructor.
   */
  public PolicyRecord() {
  }

  private String id;
  private Map<String, Object> policy;

  /**
   * @param policyRecord json object
   * @return new instance
   */
  public static PolicyRecord fromJson(final Map<String, Object> policyRecord) {
    return new PolicyRecord(
        (String) policyRecord.get(RecordFields.Policy.POLICY_RECORD_ID),
        (Map<String, Object>) policyRecord.get(RecordFields.Policy.POLICY)
    );
  }

  /**
   * @return current record as json object
   */
  public Map<String, Object> toJson() {
    return Map.of(RecordFields.Policy.POLICY_RECORD_ID, this.id, RecordFields.Policy.POLICY, this.policy);
  }
}
