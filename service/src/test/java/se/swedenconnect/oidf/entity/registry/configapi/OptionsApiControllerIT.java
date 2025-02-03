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

package se.swedenconnect.oidf.entity.registry.configapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.service.JpaOptionsService;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

/**
 * oidf-entity-registry
 *
 * @author Per Fredrik Plars
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class OptionsApiControllerIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  JpaOptionsService jpaOptionsService;

  @Test
  void testGetOptions() {
    final OptionsRecord OptionsRecord =
        jpaOptionsService.get(FkKeyType.TRUSTMARKISSUER, "49c15858-df50-426e-ace8-99961fcfbcfd");
    System.out.println(OptionsRecord);
  }
}