/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallHelper;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.GlobalLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalLocalCacheManagerImpl implements GlobalLocalCacheManager {
  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache> localCaches             = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private final TCConcurrentMultiMap<LockID, ObjectID>           lockIdsToCdsmIds        = new TCConcurrentMultiMap<LockID, ObjectID>();
  private final LocksRecallHelper                                locksRecallHelper;
  private final GlobalL1ServerMapLocalCacheStoreListener         localCacheStoreListener = new GlobalL1ServerMapLocalCacheStoreListener();

  public GlobalLocalCacheManagerImpl(LocksRecallHelper locksRecallHelper) {
    this.locksRecallHelper = locksRecallHelper;
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

  public void addListenerToStore(L1ServerMapLocalCacheStore store) {
    localCacheStoreListener.addListener(store);
  }

  // TODO: is this method needed?
  public void removeLocalCache(ObjectID mapID) {
    localCaches.remove(mapID);
  }

  public void initiateLockRecall(Set<LockID> lockIds) {
    locksRecallHelper.initiateLockRecall(lockIds);
  }

  public void recallLocksInline(Set<LockID> lockIds) {
    locksRecallHelper.recallLocksInline(lockIds);
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
  public void removeEntriesForObjectId(ObjectID mapID, Set<ObjectID> set) {
    ServerMapLocalCache cache = localCaches.get(mapID);
    if (cache != null) {
      for (ObjectID id : set) {
        cache.removeEntriesForObjectId(id);
      }
    }
  }

  /**
   * This should be called only when recall happens
   */
  public void removeEntriesForLockId(LockID lockID) {
    final Set<ObjectID> cdsmIds = lockIdsToCdsmIds.removeAll(lockID);

    for (ObjectID mapID : cdsmIds) {
      ServerMapLocalCache localCache = localCaches.get(mapID);
      localCache.removeEntriesForLockId(lockID);
    }
  }

  public void rememberMapIdForValueLockId(LockID valueLockId, ObjectID mapID) {
    lockIdsToCdsmIds.add(valueLockId, mapID);
  }

  private class GlobalL1ServerMapLocalCacheStoreListener<K, V> implements L1ServerMapLocalCacheStoreListener<K, V> {
    private final Map<L1ServerMapLocalCacheStore, Object> storeSet = new IdentityHashMap<L1ServerMapLocalCacheStore, Object>();

    public void notifyElementEvicted(K key, V value) {
      notifyElementsEvicted(Collections.singletonMap(key, value));
    }

    public synchronized void addListener(L1ServerMapLocalCacheStore store) {
      if (!storeSet.containsKey(store)) {
        storeSet.put(store, null);
        store.addListener(this);
      }
    }

    // TODO: does this need to be present in the interface? not called from outside
    public void notifyElementsEvicted(Map<K, V> evictedElements) {
      // TODO: should the flushing logic be done inside another thread, since this might delay "put" if eviction called
      // within that thread
      Set<Map.Entry<K, V>> entries = evictedElements.entrySet();

      for (Entry entry : entries) {
        if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) {
          // TODO: log warn here?
          continue;
        }

        AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();
        ObjectID mapID = value.getMapID();
        ServerMapLocalCache localCache = localCaches.get(mapID);
        if (localCache != null) {
          // the entry has been already removed from the local store, this will remove the id->key mapping if it exists
          localCache.evictedFromStore(value.getId(), entry.getKey());
        }
      }
    }

  }
}
