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

import org.springframework.stereotype.Service;
import se.swedenconnect.oidf.entity.registry.entity.FkKeyType;
import se.swedenconnect.oidf.registry.api.model.OptionsRecord;

import java.util.List;
import java.util.UUID;

/**
 * OptionsCRUDSelector serves as a selector for delegating the operations defined in the OptionsCRUD interface to the
 * appropriate implementation based on the provided FkKeyType. It maintains a list of OptionsCRUD implementations and
 * determines the correct one to use for each operation.
 *
 * @author Per Fredrik Plars
 */
@Service
public class OptionsCRUDSelector implements OptionsCRUD {

  final List<OptionsCRUD> optionsCRUDS;

  /**
   * Constructs an OptionsCRUDSelector with a list of OptionsCRUD implementations. The provided list is used to delegate
   * operations to the appropriate implementation based on the corresponding FkKeyType.
   *
   * @param optionsCRUDS a list of implementations of the OptionsCRUD interface that this selector will use to
   *     delegate operations.
   */
  public OptionsCRUDSelector(final List<OptionsCRUD> optionsCRUDS) {
    this.optionsCRUDS = optionsCRUDS;
  }

  private OptionsCRUD getOptionsCRUD(final FkKeyType fkKeyType) {
    return this.optionsCRUDS.stream()
        .filter(optionsCRUD -> optionsCRUD.supports(fkKeyType))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No optionsCRUD found for::" + fkKeyType));
  }

  @Override
  public boolean supports(final FkKeyType fkKeyType) {
    return this.getOptionsCRUD(fkKeyType).supports(fkKeyType);
  }

  @Override
  public OptionsRecord create(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    return this.getOptionsCRUD(fkKeyType).create(fkKeyType, id, record);
  }

  @Override
  public OptionsRecord update(final FkKeyType fkKeyType, final UUID id, final OptionsRecord record) {
    return this.getOptionsCRUD(fkKeyType).update(fkKeyType, id, record);
  }

  @Override
  public OptionsRecord get(final FkKeyType fkKeyType, final UUID id) {
    return this.getOptionsCRUD(fkKeyType).get(fkKeyType, id);
  }

  @Override
  public OptionsRecord template(final FkKeyType fkKeyType) {
    return this.getOptionsCRUD(fkKeyType).template(fkKeyType);
  }

  @Override
  public void delete(final FkKeyType fkKeyType, final UUID id) {
    this.getOptionsCRUD(fkKeyType).delete(fkKeyType, id);
  }

}
