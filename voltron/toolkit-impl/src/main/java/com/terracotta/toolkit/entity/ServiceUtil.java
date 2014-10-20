package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityCreationService;

import java.lang.reflect.Field;

/**
 * @author twu
 */
public class ServiceUtil {
  public static <T extends Entity, S extends EntityCreationService>  Class<S> getServiceClass(Class<T> typeClass) {
    try {
      final Field service = typeClass.getDeclaredField("SERVICE");
      return (Class<S>) service.get(typeClass);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    throw new RuntimeException("nope");
  }
}
