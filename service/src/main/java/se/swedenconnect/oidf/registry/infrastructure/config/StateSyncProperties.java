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
package se.swedenconnect.oidf.registry.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;

/**
 * Settings of the change log that the federation services read their changes from.
 *
 * @param pageSize the maximum number of changes of a page of {@code /changes}. Unset gives
 *     {@value #DEFAULT_PAGE_SIZE}
 * @param retention how long a {@code delete} change is kept. A federation service that has not read the changes for
 *     longer reads all changes again from 0. Unset gives {@value #DEFAULT_RETENTION}
 * @param compactionInterval how often the change log is compacted. Unset gives
 *     {@value #DEFAULT_COMPACTION_INTERVAL}
 * @author Felix Hellman
 */
@ConfigurationProperties("openid.federation.registry.state-sync")
public record StateSyncProperties(Integer pageSize, Duration retention, Duration compactionInterval) {

  /** The page size when {@code page-size} is not configured. */
  public static final int DEFAULT_PAGE_SIZE = 1000;

  /** The retention when {@code retention} is not configured. */
  public static final String DEFAULT_RETENTION = "P7D";

  /** The compaction interval when {@code compaction-interval} is not configured. */
  public static final String DEFAULT_COMPACTION_INTERVAL = "PT1H";

  /**
   * The maximum number of changes of a page.
   *
   * @return the configured page size, or the default
   */
  public int effectivePageSize() {
    if (this.pageSize == null) {
      return DEFAULT_PAGE_SIZE;
    }
    return this.pageSize;
  }

  /**
   * How long a {@code delete} change is kept.
   *
   * @return the configured retention, or the default
   */
  public Duration effectiveRetention() {
    if (this.retention == null) {
      return Duration.parse(DEFAULT_RETENTION);
    }
    return this.retention;
  }

  /**
   * How often the change log is compacted.
   *
   * @return the configured interval, or the default
   */
  public Duration effectiveCompactionInterval() {
    if (this.compactionInterval == null) {
      return Duration.parse(DEFAULT_COMPACTION_INTERVAL);
    }
    return this.compactionInterval;
  }

  /**
   * Checks that the page size and the durations are positive.
   */
  @PostConstruct
  public void validate() {
    Assert.isTrue(this.effectivePageSize() > 0,
        "openid.federation.registry.state-sync.page-size must be positive");
    Assert.isTrue(this.effectiveRetention().isPositive(),
        "openid.federation.registry.state-sync.retention must be positive");
    Assert.isTrue(this.effectiveCompactionInterval().isPositive(),
        "openid.federation.registry.state-sync.compaction-interval must be positive");
  }
}
