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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.entity.PolicyEntity;
import se.swedenconnect.oidf.entity.registry.repository.PolicyRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("h2")
class PolicyRepositoryTest {

  @Autowired
  private PolicyRepository policyRepository;

  @Test
  public void testSaveAndFindPolicy() {
    final PolicyEntity policy = new PolicyEntity();

    policy.setPolicyId(UUID.randomUUID());
    this.policyRepository.save(policy);

    final PolicyEntity foundPolicy = this.policyRepository.findById(policy.getPolicyId()).orElse(null);

    assertThat(foundPolicy).isNotNull();
    assertThat(foundPolicy.getPolicyId()).isEqualTo(policy.getPolicyId());

    final PolicyEntity foundPolicyByExtID = this.policyRepository.findById(policy.getPolicyId())
        .orElse(null);
    System.out.println(foundPolicyByExtID);
    assertThat(foundPolicyByExtID).isNotNull();
    assertThat(foundPolicy.getLastModifiedDate()).isNotNull();
    assertThat(foundPolicy.getCreatedDate()).isNotNull();

  }

}