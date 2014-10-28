package org.terracotta.entity;

import org.terracotta.toolkit.entity.Entity;

import java.util.ServiceLoader;

/**
 * @author twu
 */
public class EntityCreationServiceFactory {
  public static <T extends Entity> EntityCreationService<T> creationServiceForType(Class<T> cls) {
    ServiceLoader<EntityCreationService> serviceLoader = ServiceLoader.load(EntityCreationService.class,
        EntityCreationServiceFactory.class.getClassLoader());
    for (EntityCreationService entityCreationService : serviceLoader) {
      if (entityCreationService.handlesEntityType(cls)) {
        return entityCreationService;
      }
    }
    throw new IllegalArgumentException("Can't handle entity type " + cls.getName());
  }
}
