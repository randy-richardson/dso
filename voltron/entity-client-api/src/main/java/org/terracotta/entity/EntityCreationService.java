package org.terracotta.entity;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityConfiguration;

/**
 * @author twu
 */
public interface EntityCreationService<T extends Entity> {
  /**
   * Check if this service handles the given entity type.
   *
   * @param cls type to check
   * @return true if this service does handle the given type
   */
  boolean handlesEntityType(Class<T> cls);

  /**
   * Create an entity of the given type.
   *
   * @param endpoint RPC endpoint for the entity
   * @param configuration entity specific configuration
   * @return entity
   */
  T create(final EntityClientEndpoint endpoint, EntityConfiguration configuration);
}
