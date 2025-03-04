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
package se.swedenconnect.oidf.entity.registry.audit;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.event.EventListener;

/**
 * A listener that processes audit events and logs them based on the configured log level. The log level can be set
 * dynamically via configuration and supports levels such as INFO, DEBUG, TRACE, or NONE.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
public class AuditEventLogListener {

  @Value("${auditlogging.loglevel:INFO}")
  String loglevel;

  /**
   * Handles and processes an audit event by logging it based on the configured log level. The method listens for
   * incoming {@link AuditEvent} instances and applies the logging configuration, which can be set to INFO, DEBUG,
   * TRACE, or NONE. If an invalid log level is configured, default behavior logs at the DEBUG level.
   *
   * @param event the {@code AuditEvent} to be processed and logged. This object contains information about the
   *     audit event, including its type and associated data.
   */
  @EventListener
  public void onAuditEvent(final AuditApplicationEvent event) {
    switch (this.loglevel.toLowerCase()) {
    case "debug":
      log.debug("{}", event);
      break;
    case "trace":
      log.trace("{}", event);
      break;
    case "none":
      break;
    default:
      log.info("{}", event);
      break;
    }
  }

  /**
   * Performs validation of the configured log level upon bean initialization. Ensures that the log level is set to one
   * of the supported values: INFO, DEBUG, TRACE, or NONE. If an invalid value is detected, a warning is logged and the
   * default value (INFO) is applied.
   */
  @PostConstruct
  public void validate() {
    if (!this.loglevel.matches("^(INFO|DEBUG|TRACE|NONE)$")) {
      log.warn("Invalid loglevel configured: {}. Using INFO as default", this.loglevel);
    }
  }

}
