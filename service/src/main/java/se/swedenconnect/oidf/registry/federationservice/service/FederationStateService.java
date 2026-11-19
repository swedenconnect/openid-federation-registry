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
package se.swedenconnect.oidf.registry.federationservice.service;

import com.nimbusds.jose.shaded.gson.JsonArray;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonNull;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonPrimitive;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.registry.federationservice.state.StateChangeStore;
import se.swedenconnect.oidf.registry.federationservice.state.StateKey;
import se.swedenconnect.oidf.registry.infrastructure.config.StateSyncProperties;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serves the changes of the records of an instance, each record under a key, so that a federation service can read
 * only what has changed since it last read. A page is read in one transaction, and the record of a change is built
 * from the state of the registry at that time, which may be newer than the change.
 *
 * @author Felix Hellman
 */
@Service
public class FederationStateService {

  private static final String CHANGES_CLAIM = "state_changes";

  private final OidfApiService oidfApiService;
  private final StateChangeStore store;
  private final int pageSize;

  /**
   * Constructor.
   *
   * @param oidfApiService builds and signs the records
   * @param store the change log
   * @param properties the page size
   */
  public FederationStateService(final OidfApiService oidfApiService, final StateChangeStore store,
      final StateSyncProperties properties) {
    this.oidfApiService = oidfApiService;
    this.store = store;
    this.pageSize = properties.effectivePageSize();
  }

  /**
   * A page of the changes with {@code seq > since}, ordered by sequence number. An {@code upsert} carries the record
   * as it is now, and a key that builds to nothing is given as a {@code delete}. {@code next} is the last sequence
   * number of the page when more changes exist, else {@code null}. {@code full} is true when {@code since} is 0 or
   * below the highest sequence number of the removed {@code delete} changes: the pages from 0 then hold every live key.
   *
   * @param instanceId the instance ID
   * @param since the sequence number the reader has read up to
   * @param plain whether to return plain JSON
   * @return the page, as JSON or as the claim {@value #CHANGES_CLAIM} of a signed JWT
   * @throws ResponseStatusException 400 if {@code since} is negative
   */
  @Transactional(readOnly = true)
  public String changes(final UUID instanceId, final long since, final boolean plain) {
    if (since < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "since must not be negative");
    }
    this.oidfApiService.requireInstance(instanceId);
    final boolean full = since == 0 || since < this.store.prunedSeq(instanceId);
    final List<StateChangeStore.Change> changes = this.store.changes(instanceId, since, this.pageSize + 1);
    final List<StateChangeStore.Change> page = changes.subList(0, Math.min(this.pageSize, changes.size()));

    final JsonArray items = new JsonArray();
    page.forEach(change -> {
      final JsonObject item = new JsonObject();
      Optional<JsonElement> record = Optional.empty();
      if (StateKey.UPSERT.equals(change.op())) {
        record = this.oidfApiService.findRecord(instanceId, change.key());
      }
      String op = StateKey.DELETE;
      if (record.isPresent()) {
        op = StateKey.UPSERT;
      }
      item.addProperty("seq", change.seq());
      item.addProperty("op", op);
      item.addProperty("key", change.key());
      record.ifPresent(element -> item.add("record", element));
      items.add(item);
    });
    final JsonObject payload = new JsonObject();
    JsonElement next = JsonNull.INSTANCE;
    if (changes.size() > this.pageSize) {
      next = new JsonPrimitive(page.getLast().seq());
    }
    payload.add("next", next);
    payload.addProperty("full", full);
    payload.add("changes", items);
    return this.oidfApiService.toResponse(CHANGES_CLAIM, this.oidfApiService.toJson(payload), plain);
  }
}
