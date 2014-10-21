package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.collections.ClusteredMap;
import org.terracotta.toolkit.entity.Entity;

import com.terracotta.toolkit.collections.ClusteredMapService;

import java.lang.reflect.Field;

/**
 * @author twu
 */
public class ServiceUtil {
  public static <T extends Entity, S extends EntityCreationService>  Class<S> getServiceClass(Class<T> typeClass) {
    // TODO: this should probably not be hard coded like so...
    if (typeClass == ClusteredMap.class) {
      return (Class<S>) ClusteredMapService.class;
    }
    throw new IllegalArgumentException("Dunno what to do with " + typeClass);
  }
}
