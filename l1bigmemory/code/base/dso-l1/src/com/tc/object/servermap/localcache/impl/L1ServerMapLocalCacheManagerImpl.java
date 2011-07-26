/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.Sink;
import com.tc.exception.TCRuntimeException;
import com.tc.invalidation.Invalidations;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfRemovedFromStoreCallback;
import com.tc.object.TCObjectSelfStoreValue;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.ServerMapLocalCacheRemoveCallback;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class L1ServerMapLocalCacheManagerImpl implements L1ServerMapLocalCacheManager {

  private final ConcurrentHashMap<ObjectID, ServerMapLocalCache>                   localCaches             = new ConcurrentHashMap<ObjectID, ServerMapLocalCache>();
  private final TCConcurrentMultiMap<LockID, ObjectID>                             lockIdsToCdsmIds        = new TCConcurrentMultiMap<LockID, ObjectID>();
  private final Map<L1ServerMapLocalCacheStore, L1ServerMapLocalStoreEvictionInfo> stores                  = new IdentityHashMap<L1ServerMapLocalCacheStore, L1ServerMapLocalStoreEvictionInfo>();
  private final GlobalL1ServerMapLocalCacheStoreListener                           localCacheStoreListener = new GlobalL1ServerMapLocalCacheStoreListener();
  private final AtomicBoolean                                                      shutdown                = new AtomicBoolean();
  private final LocksRecallService                                                 locksRecallHelper;
  private final Sink                                                               capacityEvictionSink;
  private final RemoveCallback                                                     removeCallback;

  private final ObjectIDSet                                                        tcObjectSelfStoreOids   = new ObjectIDSet();
  private final ReentrantReadWriteLock                                             tcObjectStoreLock       = new ReentrantReadWriteLock();
  private final AtomicInteger                                                      tcObjectSelfStoreSize   = new AtomicInteger();
  private volatile TCObjectSelfRemovedFromStoreCallback                            tcObjectSelfRemovedFromStoreCallback;

  // private final Sink ttittlExpiredSink;

  public L1ServerMapLocalCacheManagerImpl(LocksRecallService locksRecallHelper, Sink capacityEvictionSink,
                                          Sink ttittlExpiredSink) {
    this.locksRecallHelper = locksRecallHelper;
    this.capacityEvictionSink = capacityEvictionSink;
    // this.ttittlExpiredSink = ttittlExpiredSink;
    removeCallback = new RemoveCallback();
  }

  public void initializeTCObjectSelfStore(TCObjectSelfRemovedFromStoreCallback callback) {
    this.tcObjectSelfRemovedFromStoreCallback = callback;
  }

  public ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager, Manager manager,
                                                   boolean localCacheEnabled, TCObjectServerMap tcObjectServerMap) {
    if (shutdown.get()) {
      throwAlreadyShutdownException();
    }
    ServerMapLocalCache serverMapLocalCache = localCaches.get(mapId);
    if (serverMapLocalCache == null) {
      serverMapLocalCache = new ServerMapLocalCacheImpl(mapId, objectManager, manager, this, localCacheEnabled,
                                                        tcObjectServerMap, removeCallback);
      ServerMapLocalCache old = localCaches.putIfAbsent(mapId, serverMapLocalCache);
      if (old != null) {
        serverMapLocalCache = old;
      }
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

  public void recallLocks(Set<LockID> lockIds) {
    locksRecallHelper.recallLocks(lockIds);
  }

  public void recallLocksInline(Set<LockID> lockIds) {
    locksRecallHelper.recallLocksInline(lockIds);
  }

  public void addAllObjectIDsToValidate(Invalidations invalidations) {
    for (ServerMapLocalCache localCache : localCaches.values()) {
      localCache.addAllObjectIDsToValidate(invalidations);
    }
  }

  /**
   * This method is called only for invalidations
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
   * This method is called only when recall happens
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

  public void evictElements(Map evictedElements) {
    Set<Map.Entry> entries = evictedElements.entrySet();

    for (Entry entry : entries) {
      if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) {
        throwAssert("Eviction should not happen on pinned elements and all unpinned elements should be intances of local cache store value");
      }

      AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();
      ObjectID mapID = value.getMapID();
      ServerMapLocalCache localCache = localCaches.get(mapID);
      if (localCache != null) {
        // the entry has been already removed from the local store, this will remove the id->key mapping if it exists
        localCache.evictedFromStore(value.getId(), entry.getKey());
      } else {
        throwAssert("LocalCache not mapped for mapId: " + mapID);
      }
    }
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
      // This should be inside another thread, if not it will cause a deadlock
      L1ServerMapEvictedElementsContext context = new L1ServerMapEvictedElementsContext(
                                                                                        evictedElements,
                                                                                        L1ServerMapLocalCacheManagerImpl.this);
      capacityEvictionSink.add(context);
    }

    public void notifyElementExpired(K key, V v) {
      notifyElementEvicted(key, v);
      // AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) v;
      // ObjectID mapID = value.getMapID();
      // ServerMapLocalCache localCache = localCaches.get(mapID);
      // if (localCache != null) {
      // CachedItemExpiredContext cachedItemExpiredContext = new CachedItemExpiredContext(localCache, key, value);
      // ttittlExpiredSink.add(cachedItemExpiredContext);
      // } else {
      // throwAssert("LocalCache not mapped for mapId: " + mapID);
      // }
    }

    public void notifySizeChanged(L1ServerMapLocalCacheStore store) {
      initiateCapacityEvictionIfRequired(store);
    }

  }

  private void throwAssert(String msg) {
    throw new AssertionError(msg);
  }

  // ----------------------------------------
  // TCObjectSelfStore methods
  // ----------------------------------------

  public Object getById(ObjectID oid) {
    tcObjectStoreLock.readLock().lock();
    try {
      for (L1ServerMapLocalCacheStore store : this.stores.keySet()) {
        Object object = store.get(oid);
        if (object == null) {
          continue;
        }
        if (object instanceof TCObjectSelfStoreValue) {
          return ((TCObjectSelfStoreValue) object).getTCObjectSelf();
        } else if (object instanceof List) {
          // for eventual value invalidation, use any of them to look up the value
          List list = (List) object;
          if (list.size() <= 0) {
            // all keys have been invalidated already, return null (lookup will happen)
            return null;
          }
          AbstractLocalCacheStoreValue localCacheStoreValue = (AbstractLocalCacheStoreValue) store.get(list.get(0));
          return localCacheStoreValue == null ? null : localCacheStoreValue.asEventualValue().getValue();
        } else {
          throw new AssertionError("Unknown type mapped to oid: " + oid + ", value: " + object
                                   + ". Expected to be mapped to either of TCObjectSelfStoreValue or a List");
        }
      }
      return null;
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public Object getByIdFromStore(ObjectID oid, L1ServerMapLocalCacheStore store) {
    tcObjectStoreLock.readLock().lock();
    try {
      Object object = store.get(oid);
      if (object == null) { return null; }
      if (object instanceof TCObjectSelfStoreValue) {
        return ((TCObjectSelfStoreValue) object).getTCObjectSelf();
      } else if (object instanceof List) {
        // for eventual value invalidation, use any of them to look up the value
        List list = (List) object;
        if (list.size() <= 0) {
          // all keys have been invalidated already, return null (lookup will happen)
          return null;
        }
        AbstractLocalCacheStoreValue localCacheStoreValue = (AbstractLocalCacheStoreValue) store.get(list.get(0));
        return localCacheStoreValue == null ? null : localCacheStoreValue.asEventualValue().getValue();
      } else {
        throw new AssertionError("Unknown type mapped to oid: " + oid + ", value: " + object
                                 + ". Expected to be mapped to either of TCObjectSelfStoreValue or a List");
      }
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public void addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                              Object tcoself) {
    tcObjectStoreLock.writeLock().lock();
    try {
      if (tcoself instanceof TCObject) {
        // no need of instanceof check if tcoself is declared as TCObject only... skipping for tests.. refactor later
        tcObjectSelfStoreOids.add(((TCObject) tcoself).getObjectID());
      }
      tcObjectSelfStoreSize.incrementAndGet();
      if (!localStoreValue.isEventualConsistentValue()) {
        store.put(((TCObject) tcoself).getObjectID(), new TCObjectSelfWrapper(tcoself),
                  PutType.PINNED_NO_SIZE_INCREMENT);
      } // else no need to store another mapping as for eventual already oid->localCacheEventualValue mapping exists,
      // and actual value is present in the localCacheEventualValue
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  private void removeTCObjectSelfForId(ServerMapLocalCache serverMapLocalCache,
                                       AbstractLocalCacheStoreValue localStoreValue) {
    tcObjectStoreLock.writeLock().lock();
    try {
      ObjectID valueOid = localStoreValue.getObjectId();
      if (ObjectID.NULL_ID.equals(valueOid) || !tcObjectSelfStoreOids.contains(valueOid)) { return; }

      // some asertions... can be removed?
      Object object = serverMapLocalCache.getInternalStore().get(valueOid);
      if (localStoreValue.isEventualConsistentValue()) {
        if (object != null) {
          if (!(object instanceof List)) {
            //
            throw new AssertionError("With eventual, oid's can be mapped to List only, oid: " + valueOid
                                     + ", mapped to: " + object);
          } else {
            List list = (List) object;
            if (list.size() > 1) { throw new AssertionError(
                                                            "With eventual, oid's should be mapped to maximum of one key, oid: "
                                                                + valueOid + ", list: " + list); }
          }
        }
      } else {
        if (object != null) {
          if (!(object instanceof TCObjectSelfStoreValue)) {
            //
            throw new AssertionError("Object mapped by oid is not TCObjectSelfStoreValue, oid: " + valueOid
                                     + ", value: " + object);
          }
        }
      }

      Object removed = serverMapLocalCache.getInternalStore().remove(valueOid, RemoveType.NO_SIZE_DECREMENT);

      tcObjectSelfStoreOids.remove(valueOid);
      tcObjectSelfStoreSize.decrementAndGet();
      // TODO: remove the cast to TCObjectSelf, right now done to appease unit tests
      if (removed != null && removed instanceof TCObjectSelf) {
        this.tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore((TCObjectSelf) removed);
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public int size() {
    return tcObjectSelfStoreSize.get();
  }

  public void addAllObjectIDs(Set oids) {
    oids.addAll(this.tcObjectSelfStoreOids);
  }

  public boolean contains(ObjectID objectID) {
    return this.tcObjectSelfStoreOids.contains(objectID);
  }

  private static class TCObjectSelfWrapper implements TCObjectSelfStoreValue, Serializable {
    private final Object tcObject;

    private TCObjectSelfWrapper(Object tcObject) {
      this.tcObject = tcObject;
    }

    public Object getTCObjectSelf() {
      return tcObject;
    }

  }

  private class RemoveCallback implements ServerMapLocalCacheRemoveCallback {
    public void removedElement(Object key, AbstractLocalCacheStoreValue localStoreValue) {
      // clear the oid->value mapping from the tcoSelfStore
      ServerMapLocalCache serverMapLocalCache = localCaches.get(localStoreValue.getMapID());
      if (serverMapLocalCache == null) { throw new AssertionError("No local cache mapped for mapId: "
                                                                  + localStoreValue.getMapID()); }
      removeTCObjectSelfForId(serverMapLocalCache, localStoreValue);
    }
  }
}
