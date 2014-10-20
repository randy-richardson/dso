package com.terracotta.toolkit.entity;

import org.terracotta.toolkit.entity.Entity;
import org.terracotta.toolkit.internal.concurrent.locks.ToolkitLockTypeInternal;

import com.tc.object.locks.EntityLockID;
import com.tc.object.locks.LockID;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.concurrent.locks.ToolkitLockingApi;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author twu
 */
public class MaintenanceModeService {
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final PlatformService platformService;

  public MaintenanceModeService(final PlatformService platformService) {
    this.platformService = platformService;
  }

  public <T extends Entity> void readLockEntity(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        ToolkitLockingApi.lock(lockID, ToolkitLockTypeInternal.READ, platformService);
      }
    }));
  }

  private static void await(final Future<?> future) {
    boolean interrupted = false;
    while (true) {
      try {
        future.get();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  public <T extends Entity> void readUnlockEntity(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        ToolkitLockingApi.unlock(lockID, ToolkitLockTypeInternal.READ, platformService);
      }
    }));
  }

  public <T extends Entity> void enterMaintenanceMode(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        ToolkitLockingApi.lock(lockID, ToolkitLockTypeInternal.WRITE, platformService);
      }
    }));
  }

  public <T extends Entity> void exitMaintenanceMode(final Class<T> c, final String name) {
    await(executorService.submit(new Runnable() {
      @Override
      public void run() {
        LockID lockID = new EntityLockID(c.getName(), name);
        ToolkitLockingApi.unlock(lockID, ToolkitLockTypeInternal.WRITE, platformService);
      }
    }));
  }
}
