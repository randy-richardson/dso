package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.entity.ConfigurationMismatchException;
import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.entity.EntityConfiguration;
import org.terracotta.toolkit.entity.EntityMaintenanceRef;
import org.terracotta.toolkit.entity.EntityRef;

import com.tc.net.GroupID;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;
import com.terracotta.toolkit.concurrent.locks.UnnamedToolkitLock;

import java.util.ServiceLoader;

/**
 * @author twu
 */
public class TerracottaEntityRef<T extends Entity> implements EntityMaintenanceRef<T>, EntityRef<T> {
  private final PlatformService platformService;
  private final MaintenanceModeService maintenanceModeService;
  private final Class<T> type;
  private final String name;
  private final UnnamedToolkitLock createLock;

  private enum ReferenceState {
    FREE, IN_USE, MAINTENANCE
  }

  private T entity;
  private ReferenceState state = ReferenceState.FREE;

  public TerracottaEntityRef(final PlatformService platformService, final MaintenanceModeService maintenanceModeService, final Class<T> type, final String name) {
    this.platformService = platformService;
    this.maintenanceModeService = maintenanceModeService;
    this.type = type;
    this.name = name;
    createLock = ToolkitLockingApi.createConcurrentTransactionLock("foo", platformService);
  }

  @Override
  public synchronized T acquireEntity() {
    if (state == ReferenceState.IN_USE) {
      return entity;
    } else if (state == ReferenceState.FREE) {
      maintenanceModeService.readLockEntity(type, name);
      EntityClientEndpoint endpoint = (EntityClientEndpoint) platformService.lookupRoot(name, new GroupID(0));
      if (entity == null) {
        maintenanceModeService.readUnlockEntity(type, name);
        throw new IllegalStateException("doesn't exist");
      }
      entity = getCreationService(type).create(endpoint, endpoint.getEntityConfiguration());
      state = ReferenceState.IN_USE;
    }
    return entity;
  }

  @Override
  public T getEntity() {
    throw new UnsupportedOperationException("Implement me!");
  }

  @Override
  public T acquireEntity(final EntityConfiguration configuration) throws ConfigurationMismatchException {
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

    EntityClientEndpoint endpoint = (EntityClientEndpoint) platformService.lookupRoot(name, new GroupID(0));
    if (endpoint == null) {
      createLock.lock();
      try {
        platformService.lookupOrCreateRoot(name, new EntityClientEndpoint(type.getName(), configuration), new GroupID(0));
      } finally {
        createLock.unlock(); // TODO: This should probably be synchronous in some way
      }
    } else {
      throw new IllegalStateException("Already exists");
    }
    entity = getCreationService(type).create(endpoint, configuration);
  }

  @Override
  public synchronized void exitMaintenanceMode() {
    checkMaintenanceMode();
    maintenanceModeService.exitMaintenanceMode(type, name);
    state = ReferenceState.FREE;
  }

  public synchronized EntityMaintenanceRef<T> enterMaintenanceMode() {
    if (state != ReferenceState.FREE) {
      throw new IllegalStateException("Reference is not free to enter maintenance mode.");
    }
    maintenanceModeService.enterMaintenanceMode(type, name);
    state = ReferenceState.MAINTENANCE;
    return this;
  }

  private void checkMaintenanceMode() {
    if (state != ReferenceState.MAINTENANCE) {
      throw new IllegalStateException("Not in maintenance mode");
    }
  }

  private static <T extends Entity> EntityCreationService getCreationService(Class<T> type) {
    ServiceLoader<EntityCreationService> loader = ServiceLoader.load(ServiceUtil.getServiceClass(type),
        TerracottaEntityRef.class.getClassLoader());
    for (EntityCreationService entityCreationService : loader) {
      return entityCreationService;
    }
    throw new UnsupportedOperationException("Don't have a service to handle type " + type.getName());
  }
}
