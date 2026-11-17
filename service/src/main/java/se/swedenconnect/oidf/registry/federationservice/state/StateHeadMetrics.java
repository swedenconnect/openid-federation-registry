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
package se.swedenconnect.oidf.registry.federationservice.state;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;
import se.swedenconnect.oidf.registry.infrastructure.config.RegistryProperties;

/**
 * Publishes the head of the change log of each configured instance as the gauge {@value #NAME}, tagged with the
 * instance ID. The value is read from the database, so every registry node reports the same head. A federation
 * service publishes the sequence number it has applied, so the difference shows how far it is behind.
 *
 * @author Felix Hellman
 */
@Component
public class StateHeadMetrics implements MeterBinder {

  /** The name of the gauge. */
  public static final String NAME = "oidf.registry.state.head";

  private final StateChangeStore store;
  private final RegistryProperties registryProperties;

  /**
   * Constructor.
   *
   * @param store the change log
   * @param registryProperties the configured instances
   */
  public StateHeadMetrics(final StateChangeStore store, final RegistryProperties registryProperties) {
    this.store = store;
    this.registryProperties = registryProperties;
  }

  @Override
  public void bindTo(final MeterRegistry registry) {
    this.registryProperties.instances().forEach(instance ->
        Gauge.builder(NAME, () -> this.store.head(instance.instanceId()))
            .description("Highest sequence number in the change log of the instance")
            .tag("instance", instance.instanceId().toString())
            .register(registry));
  }
}
