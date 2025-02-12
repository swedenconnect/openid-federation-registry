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
package se.swedenconnect.oidf.entity.registry.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.entity.registry.entity.InstanceEntity;
import se.swedenconnect.oidf.entity.registry.repository.InstanceRepository;
import se.swedenconnect.oidf.entity.registry.repository.SettingsRepository;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;
import se.swedenconnect.oidf.registry.api.model.Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static se.swedenconnect.oidf.entity.registry.entity.FkKeyType.INSTANCE;

/**
 * OptionsCRUDPolices is a service that extends the OptionsCRUDAdapter to perform Create, Read, Update, and Delete
 * (CRUD) functionalities specifically for modules. It operates on various types of modules utilizing FkKeyType and
 * manages their interactions with repositories.
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@Service
public class OptionsCRUDInstance extends OptionsCRUDAdapter {

  private final InstanceRepository instanceRepository;

  /**
   * Constructor for the OptionsCRUDInstance class.
   *
   * @param instanceRepository The primary repository for managing instance data.
   * @param settingsRepository The repository for managing settings data.
   * @param instanceRepository1 An additional instance repository used for specific operations.
   */
  public OptionsCRUDInstance(final InstanceRepository instanceRepository, final SettingsRepository settingsRepository,
      final InstanceRepository instanceRepository1) {
    super(instanceRepository, settingsRepository);
    this.instanceRepository = instanceRepository1;
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return INSTANCE == fkKeyType;
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not supported");
  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not supported");
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    final InstanceEntity entity = this.instanceRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No data found for:%s %s".formatted(fkKeyType, id)));

    final List<Values> options = new ArrayList<>();
    options.add(Values.builder().key("name").value(entity.getName()).build());
    options.add(Values.builder().key("id").value(entity.getInstanceId().toString()).build());
    return new OptionsRecord.Builder()
        .option(options)
        .build();
  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not supported");
  }

  @Override
  public OptionsRecord delete(final FkKeyType fkKeyType, final UUID id) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not supported");
  }

  @Override
  public List<Map<String, Object>> list(final FkKeyType fkKeyType) {
    return this.instanceRepository.findAll()
        .stream()
        .map(entity ->
            Map.of("name", entity.getName(),
                "id", (Object) entity.getInstanceId()))
        .toList();
  }

}

