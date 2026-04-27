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
package se.swedenconnect.oidf.registry.registrations.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.swedenconnect.oidf.registry.registrations.model.RegistrationStatus;
import se.swedenconnect.oidf.registry.registrations.repository.RegistrationRepository;

import java.time.LocalDateTime;

/**
 * Scheduled job that deletes {@code REJECTED} registration records older than 30 days.
 *
 * @author Per Fredrik Plars
 */
@Component
public class RegistrationCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(RegistrationCleanupJob.class);

  private final RegistrationRepository registrationRepository;

  public RegistrationCleanupJob(final RegistrationRepository registrationRepository) {
    this.registrationRepository = registrationRepository;
  }

  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void deleteExpiredRejections() {
    final LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    log.info("Deleting REJECTED registrations older than {}", cutoff);
    this.registrationRepository.deleteByStatusAndCreatedDateBefore(RegistrationStatus.REJECTED, cutoff);
  }
}