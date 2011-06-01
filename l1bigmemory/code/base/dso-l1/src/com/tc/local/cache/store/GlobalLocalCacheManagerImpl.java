/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.ObjectID;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.locks.LockID;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalLocalCacheManagerImpl implements GlobalLocalCacheManager {
  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache> localCaches = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private volatile RemoteServerMapManager                        serverMapManager;

  public void initialize(RemoteServerMapManager serverManager) {
    this.serverMapManager = serverManager;
  }

  public void addLocalCache(ObjectID mapID, ServerMapLocalCache cache) {
    localCaches.put(mapID, cache);
  }

  public void recallLocks(Set<LockID> toEvict) {
    serverMapManager.recallLocks(toEvict);
  }

  public void removeLocalCache(ObjectID mapID) {
    localCaches.remove(mapID);
  }

  public Map addAllObjectIDsToValidate(Map map) {
    for (ServerMapLocalCache localCache : localCaches.values()) {
      localCache.addAllObjectIDsToValidate(map);
    }
    return map;
  }

  public void flush(ObjectID mapID, Set<ObjectID> set) {
    ServerMapLocalCache cache = localCaches.get(mapID);

    for (ObjectID id : set) {
      cache.flush(id);
    }
  }

  public void flush(LockID lockID) {
    // throw new ImplementMe();
  }
}
