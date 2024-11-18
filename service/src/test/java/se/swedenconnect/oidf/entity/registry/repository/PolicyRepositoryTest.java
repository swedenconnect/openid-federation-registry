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
package se.swedenconnect.oidf.entity.registry.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.model.PolicyEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class PolicyRepositoryTest {

  @Autowired
  private PolicyRepository policyRepository;

  @Test
  public void testSaveAndFindPolicy() {
    PolicyEntity policy = new PolicyEntity();
    policy.setName("Test Policy");
    policy.setPolicy("{ \"key\": \"value\" }");

    policyRepository.save(policy);

    PolicyEntity foundPolicy = policyRepository.findById(policy.getId()).orElse(null);

    assertThat(foundPolicy).isNotNull();
    assertThat(foundPolicy.getId()).isEqualTo(policy.getId());
    assertThat(foundPolicy.getName()).isEqualTo(policy.getName());
    assertThat(foundPolicy.getPolicy()).isEqualTo(policy.getPolicy());
  }

  @Test
  public void testUniqueNameConstraint() {
    PolicyEntity policy1 = new PolicyEntity();
    policy1.setName("Unique Policy");
    policy1.setPolicy("{ \"key\": \"value1\" }");

    policyRepository.save(policy1);

    PolicyEntity policy2 = new PolicyEntity();
    policy2.setName("Unique Policy");
    policy2.setPolicy("{ \"key\": \"value2\" }");

    // Expect a DataIntegrityViolationException as the unique constraint is violated
    assertThatThrownBy(() -> policyRepository.saveAndFlush(policy2)).isInstanceOf(DataIntegrityViolationException.class);
  }

}