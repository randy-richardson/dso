package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.entity.ConfigurationMismatchException;
import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityConfiguration;
import org.terracotta.toolkit.entity.EntityMaintenanceRef;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.locks.EntityLockID;
import com.tc.object.locks.LockID;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;

/**
 * @author twu
 */
public class TerracottaEntityRef<T extends Entity> implements EntityMaintenanceRef<T> {
  private final PlatformService platformService;
  private final Class<T> type;
  private final String name;
  private final LockID maintenanceLockID;

  private enum ReferenceState {
    FREE, IN_USE, MAINTENANCE
  }

  private ReferenceState state = ReferenceState.FREE;

  public TerracottaEntityRef(final PlatformService platformService, final Class<T> type, final String name) {
    this.platformService = platformService;
    this.type = type;
    this.name = name;
    maintenanceLockID = new EntityLockID(type.getName(), name);
  }

  @Override
  public synchronized T get() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public T get(final EntityConfiguration configuration) throws ConfigurationMismatchException {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public synchronized void destroy() {
    checkMaintenanceMode();
    // success! you can't create, so obviously destruction works by default.
  }

  @Override
  public synchronized void create(final EntityConfiguration configuration) {
    checkMaintenanceMode();
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public synchronized void exitMaintenanceMode() {
    checkMaintenanceMode();
    ToolkitLockingApi.unlock(maintenanceLockID, ToolkitLockTypeInternal.WRITE, platformService);
    state = ReferenceState.FREE;
  }

  @Override
  public synchronized EntityMaintenanceRef<T> enterMaintenanceMode() {
    if (state != ReferenceState.FREE) {
      throw new IllegalStateException("Reference is not free to enter maintenance mode.");
    }
    // Should there be 1 lock per-stripe?
    ToolkitLockingApi.lock(maintenanceLockID, ToolkitLockTypeInternal.WRITE, platformService);
    state = ReferenceState.MAINTENANCE;
    return this;
  }

  private void checkMaintenanceMode() {
    if (state != ReferenceState.MAINTENANCE) {
      throw new IllegalStateException("Not in maintenance mode");
    }
  }
}
