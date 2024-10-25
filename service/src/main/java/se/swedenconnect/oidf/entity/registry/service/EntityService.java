package se.swedenconnect.oidf.entity.registry.service;

import se.swedenconnect.oidf.registry.api.model.Entity;

/**
 * EntityService is an interface that extends the CrudService interface and provides
 * CRUD operations specifically for managing {@link Entity} objects identified by a String.
 *
 * @author David Goldring
 */
public interface EntityService extends CrudService<Entity, String> {
}
