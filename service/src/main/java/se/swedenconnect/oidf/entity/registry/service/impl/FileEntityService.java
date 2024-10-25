/*
 * Copyright 2024 Sweden Connect
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
package se.swedenconnect.oidf.entity.registry.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import se.swedenconnect.oidf.entity.registry.service.EntityService;
import se.swedenconnect.oidf.registry.api.model.Entity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The FileEntityService class provides an implementation of the EntityService interface for
 * managing entity objects using a file-based storage mechanism. This service allows for creating,
 * retrieving, updating, and deleting entities while persisting the data in a file.
 * <p>
 * The data is stored in a file defined by FILE_PATH and is loaded into a in-memory storage on
 * initialization. Any changes to the entities are saved back to the file to ensure persistence.
 *
 * @author David Goldring
 */
public class FileEntityService implements EntityService {

  /**
   * The file path where the entity data is stored.
   * This constant is used by the FileEntityService to load and save entity data
   * from/to the specified file.
   * <p>
   * TODO: should be driven by configuration, if file storage should be an option.
   */
  public static final String FILE_PATH = "entity.dat";
  private final Map<String, Entity> storage;

  /**
   * Constructs a new FileEntityService and initializes the storage by loading data from a file.
   * This service manages entity objects using a file-based storage mechanism.
   */
  public FileEntityService() {
    this.storage = this.loadFromFile();
  }

  @Override
  public Entity create(final Entity entity) {
    Entity present = this.get(entity.getSubject());
    if (present != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Entity already exists");
    }

    this.storage.put(String.valueOf(entity.getSubject()), entity);
    this.saveToFile();
    return entity;
  }

  @Override
  public Entity get(final String s) {
    return this.storage.get(s);
  }

  @Override
  public List<Entity> getAll() {
    return new ArrayList<>(this.storage.values());
  }

  @Override
  public Entity update(final String s, final Entity entity) {
    if (!this.storage.containsKey(s)) {
      return null;
    }
    this.storage.put(s, entity);
    this.saveToFile();
    return entity;
  }

  @Override
  public void delete(final String s) {
    this.storage.remove(s);
    this.saveToFile();
  }

  /**
   * Saves the current in-memory storage to a file specified by {@code FILE_PATH}.
   *
   * @throws RuntimeException if an I/O error occurs while saving the data to the file
   */
  void saveToFile() {
    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
      objectOutputStream.writeObject(this.storage);
      objectOutputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Failed to save data to file", e);
    }
  }

  /**
   * Loads the entity data from a file specified by {@code FILE_PATH}.
   * If the file is not found, it initializes an empty storage.
   *
   * @return a map containing the entity data loaded from the file. If the file is not found, returns an empty map.
   */
  Map<String, Entity> loadFromFile() {
    try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
      return (Map<String, Entity>) objectInputStream.readObject();
    } catch (FileNotFoundException e) {
      return new HashMap<>();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Failed to load data from file", e);
    }
  }

}
