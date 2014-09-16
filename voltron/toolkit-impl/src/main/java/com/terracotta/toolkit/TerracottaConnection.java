package com.terracotta.toolkit;

import org.terracotta.toolkit.connection.Connection;
import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityRef;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.entity.TerracottaEntityRef;

import java.util.Collection;

/**
 * @author twu
 */
public class TerracottaConnection implements Connection {
  private final PlatformService platformService;
  private final Runnable shutdown;

  private boolean isShutdown = false;

  public TerracottaConnection(final PlatformService platformService, final Runnable shutdown) {
    this.platformService = platformService;
    this.shutdown = shutdown;
  }

  @Override
  public synchronized <T extends Entity> EntityRef<T> getEntityRef(final Class<T> cls, final String name) {
    checkShutdown();
    return new TerracottaEntityRef<T>(platformService, cls, name);
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
}
