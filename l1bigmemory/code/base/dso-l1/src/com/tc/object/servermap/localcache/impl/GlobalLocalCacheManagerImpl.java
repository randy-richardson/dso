/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
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
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalLocalCacheManagerImpl implements GlobalLocalCacheManager {

  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache>                   localCaches             = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private final TCConcurrentMultiMap<LockID, ObjectID>                             lockIdsToCdsmIds        = new TCConcurrentMultiMap<LockID, ObjectID>();
  private final Map<L1ServerMapLocalCacheStore, L1ServerMapLocalStoreEvictionInfo> stores                  = new IdentityHashMap<L1ServerMapLocalCacheStore, L1ServerMapLocalStoreEvictionInfo>();
  private final GlobalL1ServerMapLocalCacheStoreListener                           localCacheStoreListener = new GlobalL1ServerMapLocalCacheStoreListener();
  private final AtomicBoolean                                                      shutdown                = new AtomicBoolean();
  private final LocksRecallHelper                                                  locksRecallHelper;
  private final Sink                                                               capacityEvictionSink;

  public GlobalLocalCacheManagerImpl(LocksRecallHelper locksRecallHelper, Sink capacityEvictionSink) {
    this.locksRecallHelper = locksRecallHelper;
    this.capacityEvictionSink = capacityEvictionSink;
  }

  public ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager, Manager manager,
                                                   boolean localCacheEnabled) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }
    ServerMapLocalCache serverMapLocalCache = localCaches.get(mapId);
    if (serverMapLocalCache == null) {
      serverMapLocalCache = new ServerMapLocalCacheImpl(mapId, objectManager, manager, this, localCacheEnabled);
      localCaches.put(mapId, serverMapLocalCache);
    }
    return serverMapLocalCache;
  }

  public void addStoreListener(L1ServerMapLocalCacheStore store) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }
    synchronized (stores) {
      if (!stores.containsKey(store)) {
        store.addListener(localCacheStoreListener);
        stores.put(store, new L1ServerMapLocalStoreEvictionInfo(store));
      }
    }
  }

  private void throwAlreadyShutdownException() {
    throw new TCRuntimeException("GlobalCacheManager is already shut down.");
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

  public void shutdown() {
    shutdown.set(true);
    for (L1ServerMapLocalCacheStore store : stores.keySet()) {
      store.clear();
    }
  }

  private void initiateCapacityEvictionIfRequired(L1ServerMapLocalCacheStore store) {
    L1ServerMapLocalStoreEvictionInfo evictionInfo = stores.get(store);
    if (evictionInfo == null) { throw new AssertionError(); }

    if (evictionInfo.attemptEvictionStart()) {
      capacityEvictionSink.add(evictionInfo);
    }
  }

  private class GlobalL1ServerMapLocalCacheStoreListener<K, V> implements L1ServerMapLocalCacheStoreListener<K, V> {

    public void notifyElementEvicted(K key, V value) {
      notifyElementsEvicted(Collections.singletonMap(key, value));
    }

    // TODO: does this need to be present in the interface? not called from outside
    public void notifyElementsEvicted(Map<K, V> evictedElements) {
      // TODO: should the flushing logic be done inside another thread, since this might delay "put" if eviction called
      // within that thread
      Set<Map.Entry<K, V>> entries = evictedElements.entrySet();

      for (Entry entry : entries) {
        if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) {
          wtf("Eviction should not happen on pinned elements and all unpinned elements should be intances of local cache store value");
        }

        AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();
        ObjectID mapID = value.getMapID();
        ServerMapLocalCache localCache = localCaches.get(mapID);
        if (localCache != null) {
          // the entry has been already removed from the local store, this will remove the id->key mapping if it exists
          localCache.evictedFromStore(value.getId(), entry.getKey());
        } else {
          wtf("LocalCache not mapped for mapId: " + mapID);
        }
      }
    }

    public void notifyElementExpired(K key, V value) {
      // TODO
      // handle same as eviction
      this.notifyElementEvicted(key, value);
    }

    public void notifySizeChanged(L1ServerMapLocalCacheStore store) {
      initiateCapacityEvictionIfRequired(store);
    }

    // What a terrible failure! ;-)
    // Ha Ha Ha
    private void wtf(String msg) {
      throw new AssertionError(msg);
    }
  }
}
