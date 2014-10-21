package com.terracotta.toolkit;

import org.terracotta.toolkit.connection.Connection;
import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityRef;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.entity.MaintenanceModeService;
import com.terracotta.toolkit.entity.TerracottaEntityRef;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author twu
 */
public class TerracottaConnection implements Connection {
  private final PlatformService platformService;
  private final MaintenanceModeService maintenanceModeService;
  private final Runnable shutdown;

  private boolean isShutdown = false;

  private final Map<EntityKey<? extends Entity>, EntityRef<? extends Entity>> references = new HashMap<EntityKey<? extends Entity>, EntityRef<? extends Entity>>();

  public TerracottaConnection(final PlatformService platformService, final Runnable shutdown) {
    this.platformService = platformService;
    this.shutdown = shutdown;
    this.maintenanceModeService = new MaintenanceModeService(platformService);
  }

  @Override
  public synchronized <T extends Entity> EntityRef<T> getEntityRef(final Class<T> cls, final String name) {
    checkShutdown();
    final EntityKey<T> key = new EntityKey<T>(cls, name);
    EntityRef<T> ref = (EntityRef<T>) references.get(key);
    if (ref == null) {
      ref = new TerracottaEntityRef<T>(platformService, maintenanceModeService, cls, name);
      references.put(key, ref);
    }
    return ref;
  }

  @Override
  public synchronized <T extends Entity> Collection<EntityRef<T>> getEntityRefsOfType(final Class<T> cls) {
    checkShutdown();
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public synchronized void close() {
    checkShutdown();
    shutdown.run();
    isShutdown = true;
  }

  private void checkShutdown() {
    if (isShutdown) {
      throw new IllegalStateException("Already shut down");
    }
  }

  private static class EntityKey<T extends Entity> {
    private final Class<T> cls;
    private final String name;

    private EntityKey(final Class<T> cls, final String name) {
      this.cls = cls;
      this.name = name;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final EntityKey entityKey = (EntityKey) o;

      if (!cls.equals(entityKey.cls)) return false;
      if (!name.equals(entityKey.name)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = cls.hashCode();
      result = 31 * result + name.hashCode();
      return result;
    }
  }
}
