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

import se.swedenconnect.oidf.entity.registry.common.CrudService;
import se.swedenconnect.oidf.registry.api.model.TrustMarkSubjectRecord;

import java.util.List;

/**
 * TrustMarkSubjectService is an interface that extends the CrudService interface and provides CRUD operations
 * specifically for managing JSON Policy objects.
 *
 * @author Per Fredrk Plars
 */
public interface TrustMarkSubjectService extends CrudService<TrustMarkSubjectRecord, String> {
  /**
   * Getting all TrustMarkSubjectRecord for issuer and trustmarkid
   *
   * @param issuer Issuer entityid
   * @param trustmarkId TrustmarkId
   * @return List of TrustMarkSubjectRecord
   */
  List<TrustMarkSubjectRecord> getAll(String issuer, String trustmarkId);

}
