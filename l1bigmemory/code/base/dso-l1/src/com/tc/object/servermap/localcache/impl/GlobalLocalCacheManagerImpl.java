/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.GlobalLocalCacheManager;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalLocalCacheManagerImpl implements GlobalLocalCacheManager {
  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache> localCaches      = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private final TCConcurrentMultiMap<LockID, ObjectID>           lockIdsToCdsmIds = new TCConcurrentMultiMap<LockID, ObjectID>();
  private volatile RemoteServerMapManager                        serverMapManager;

  public void initialize(RemoteServerMapManager serverManager) {
    this.serverMapManager = serverManager;
  }

  public ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager, Manager manager,
                                                   boolean localCacheEnabled) {
    ServerMapLocalCache serverMapLocalCache = new ServerMapLocalCacheImpl(mapId, objectManager, manager, this,
                                                                          localCacheEnabled);
    ServerMapLocalCache old = localCaches.putIfAbsent(mapId, serverMapLocalCache);
    if (old != null) {
      serverMapLocalCache = old;
    }
    localCaches.put(mapId, serverMapLocalCache);
    return serverMapLocalCache;
  }

  // TODO: is this method needed?
  public void removeLocalCache(ObjectID mapID) {
    localCaches.remove(mapID);
  }

  public void recallLocks(Set<LockID> toEvict) {
    serverMapManager.recallLocks(toEvict);
  }

  public Map addAllObjectIDsToValidate(Map map) {
    for (ServerMapLocalCache localCache : localCaches.values()) {
      localCache.addAllObjectIDsToValidate(map);
    }
    return map;
  }

  /**
   * This method should be called only for invalidations
   */
  public void flush(ObjectID mapID, Set<ObjectID> set) {
    ServerMapLocalCache cache = localCaches.get(mapID);

    for (ObjectID id : set) {
      cache.flush(id);
    }
  }

  /**
   * This should be called only when recall happens
   */
  public void flush(LockID lockID) {
    final Set<ObjectID> cdsmIds = lockIdsToCdsmIds.removeAll(lockID);

    for (ObjectID mapID : cdsmIds) {
      ServerMapLocalCache localCache = localCaches.get(mapID);
      localCache.flush(lockID);
    }
  }

  public void rememberMapIdForValue(Object valueId, ObjectID mapID) {
    if (valueId instanceof LockID) {
      LockID lockID = (LockID) valueId;
      lockIdsToCdsmIds.add(lockID, mapID);
    }
  }
}
