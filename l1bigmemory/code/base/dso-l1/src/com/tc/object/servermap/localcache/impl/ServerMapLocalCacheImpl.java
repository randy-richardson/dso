/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectServerMap;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.GlobalLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener.TransactionCompleteOperation;
import com.tc.object.servermap.localcache.impl.LocalStoreKeySet.LocalStoreKeySetFilter;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ServerMapLocalCacheImpl implements ServerMapLocalCache {
  private static final TCLogger                                                     LOGGER           = TCLogging
                                                                                                         .getLogger(ServerMapLocalCacheImpl.class);
  private final static int                                                          CONCURRENCY      = 128;
  private static final LocalStoreKeySetFilter                                       IGNORE_ID_FILTER = new IgnoreIdsFilter();

  private final ObjectID                                                            mapID;
  private final GlobalLocalCacheManager                                             globalLocalCacheManager;
  private final boolean                                                             localCacheEnabled;
  private volatile L1ServerMapLocalCacheStore<Object, AbstractLocalCacheStoreValue> localStore;
  private final ClientObjectManager                                                 objectManager;
  private final Manager                                                             manager;
  private final ReentrantReadWriteLock[]                                            segmentLocks     = new ReentrantReadWriteLock[CONCURRENCY];
  private final TCObjectServerMap                                                   tcObjectServerMap;

  /**
   * Not public constructor, should be created only by the global local cache manager
   */
  ServerMapLocalCacheImpl(ObjectID mapID, ClientObjectManager objectManager, Manager manager,
                          GlobalLocalCacheManager globalLocalCacheManager, boolean islocalCacheEnbaled,
                          TCObjectServerMap tcObjectServerMap) {
    this.mapID = mapID;
    this.objectManager = objectManager;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.localCacheEnabled = islocalCacheEnbaled;
    this.tcObjectServerMap = tcObjectServerMap;
    for (int i = 0; i < segmentLocks.length; i++) {
      this.segmentLocks[i] = new ReentrantReadWriteLock();
    }
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore store) {
    this.localStore = store;
    this.globalLocalCacheManager.addStoreListener(store);
  }

  private boolean isStoreInitialized() {
    if (localStore == null) {
      LOGGER.warn("Store yet not initialized");
      return false;
    }
    return true;
  }

  private ReentrantReadWriteLock getLock(Object key) {
    int index = Math.abs(key.hashCode() % CONCURRENCY);
    return segmentLocks[index];
  }

  public void addStrongValueToCache(LockID lockId, Object key, Object value, MapOperationType mapOperation) {
    final LocalCacheStoreStrongValue localCacheValue = new LocalCacheStoreStrongValue(lockId, value, this.mapID);
    addToCache(key, localCacheValue, mapOperation);
  }

  public void addEventualValueToCache(ObjectID valueObjectId, Object key, Object value, MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectId, value,
                                                                                          this.mapID);
    addToCache(key, localCacheValue, mapOperation);
  }

  public void addIncoherentValueToCache(Object key, Object value, MapOperationType mapOperation) {
    final LocalCacheStoreIncoherentValue localCacheValue = new LocalCacheStoreIncoherentValue(value, this.mapID);
    addToCache(key, localCacheValue, mapOperation);
  }

  private void registerTransactionCompleteListener(final L1ServerMapLocalStoreTransactionCompletionListener listener) {
    if (listener == null) { throw new AssertionError("Listener cannot be null"); }
    ClientTransaction txn = this.objectManager.getTransactionManager().getCurrentTransaction();
    if (txn == null) { throw new UnlockedSharedObjectException(
                                                               "Attempt to access a shared object outside the scope of a shared lock.",
                                                               Thread.currentThread().getName(), manager.getClientID()); }
    txn.addTransactionCompleteListener(listener);
  }

  // TODO::FIXME:: There is a race for puts for same key from same vm - it races between the map.put() and
  // serverMapManager.put()
  private void addToCache(final Object key, final AbstractLocalCacheStoreValue localCacheValue,
                          final MapOperationType mapOperation) {
    if (!localCacheEnabled && !mapOperation.isMutateOperation()) {
      // local cache NOT enabled AND NOT a mutate operation, do not cache anything locally
      // for mutate ops keep in local cache till txn is complete
      return;
    }

    if (localCacheValue.isStrongConsistentValue()) {
      // Before putting we should remember the mapId for the lock Id as upon recall need to flush from these maps
      // (TODO: can lockId be potentially used by multiple maps?)
      globalLocalCacheManager.rememberMapIdForValueLockId(localCacheValue.asStrongValue().getLockId(), this.mapID);
    }

    { // scoping 'old' variable
      final AbstractLocalCacheStoreValue old;
      if (mapOperation.isMutateOperation()) {
        // put a pinned entry for mutate ops, unpinned on txn complete
        old = this.localStore.put(key, localCacheValue, PutType.PINNED);
      } else {
        old = this.localStore.put(key, localCacheValue, PutType.NORMAL);
      }
      removeIdToKeysMappingIfNecessary(old, key);
    }

    addIdToKeysMappingIfNecessary(localCacheValue, key);

    // register for transaction complete if mutate operation
    if (mapOperation.isMutateOperation()) {
      L1ServerMapLocalStoreTransactionCompletionListener listener = getTransactionCompleteListener(key, mapOperation);
      if (listener == null) { throw new AssertionError("Transaction Complete Listener cannot be null for mutate ops"); }
      registerTransactionCompleteListener(listener);
    }
  }

  private void removeIdToKeysMappingIfNecessary(final AbstractLocalCacheStoreValue localCacheValue, final Object key) {
    if (!isStoreInitialized()) { return; }

    if (localCacheValue != null) {
      if (localCacheValue.getId() != null && localCacheValue.getId() != ObjectID.NULL_ID) {

        LockID lockID = executeUnderSegmentWriteLock(localCacheValue.getId(), key, RemoveIdKeyMappingCallback.INSTANCE);
        initiateLockRecall(lockID);
      } // else: we don't add for null_id
    }
  }

  private void addIdToKeysMappingIfNecessary(final AbstractLocalCacheStoreValue localCacheValue, final Object key) {
    if (isStoreInitialized() && localCacheValue.getId() != null && localCacheValue.getId() != ObjectID.NULL_ID) {
      // we don't add for null_id
      executeUnderSegmentWriteLock(localCacheValue.getId(), key, AddIdKeyMappingCallback.INSTANCE);
    }
  }

  private L1ServerMapLocalStoreTransactionCompletionListener getTransactionCompleteListener(
                                                                                            final Object key,
                                                                                            MapOperationType mapOperation) {
    if (!mapOperation.isMutateOperation()) {
      // no listener required for non mutate ops
      return null;
    }
    final L1ServerMapLocalStoreTransactionCompletionListener txnCompleteListener;
    if (localCacheEnabled) {
      // when local cache is enabled, remove the cached value if the operation is a REMOVE, otherwise just unpin
      TransactionCompleteOperation onTransactionComplete = mapOperation.isRemoveOperation() ? TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY
          : TransactionCompleteOperation.UNPIN_ENTRY;
      txnCompleteListener = new L1ServerMapLocalStoreTransactionCompletionListener(this, key, onTransactionComplete);
    } else {
      // when local cache is disabled, always remove the cached value on txn complete
      txnCompleteListener = new L1ServerMapLocalStoreTransactionCompletionListener(
                                                                                   this,
                                                                                   key,
                                                                                   TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY);
    }
    return txnCompleteListener;
  }

  // private boolean isExpired(final CachedItem ci, final int now) {
  // final ExpirableEntry ee;
  // if ((TCObjectServerMapImpl.this.tti <= 0 && TCObjectServerMapImpl.this.ttl <= 0)
  // || (ee = ci.getExpirableEntry()) == null) { return false; }
  // return now >= ee.expiresAt(TCObjectServerMapImpl.this.tti, TCObjectServerMapImpl.this.ttl);
  // }

  /**
   * TODO: this is a very bad implementation, we need to make this better in future
   */
  public void clear() {
    if (!isStoreInitialized()) { return; }

    Set<LockID> lockIDs = executeUnderMapWriteLock(ClearAllEntriesCallback.INSTANCE);
    initiateLockRecall(lockIDs);
  }

  public void clearInline() {
    if (!isStoreInitialized()) { return; }

    Set<LockID> lockIDs = executeUnderMapWriteLock(ClearAllEntriesCallback.INSTANCE);
    initiateInlineLockRecall(lockIDs);
  }

  public int size() {
    if (!isStoreInitialized()) { return 0; }

    return this.localStore.size();
  }

  public int evictCachedEntries(int toClear) {
    return this.localStore.evict(toClear);
  }

  /**
   * When a remove from local cache is called, remove and flush
   */
  public void removeFromLocalCache(Object key) {
    if (!isStoreInitialized()) { return; }

    Object value = localStore.remove(key, RemoveType.NORMAL);
    if (value != null && value instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
      if (localValue.getId() != null) {
        // not incoherent item, remove id-key mapping
        LockID id = executeUnderSegmentWriteLock(localValue.getId(), key, RemoveEntryForKeyCallback.INSTANCE);
        initiateLockRecall(id);
      }
    }
  }

  public void evictedFromStore(Object id, Object key) {
    if (!isStoreInitialized()) { return; }

    LockID lockID = executeUnderSegmentWriteLock(id, key, RemoveEntryForKeyCallback.INSTANCE);
    initiateLockRecall(lockID);
  }

  public Set getKeySet() {
    return Collections
        .unmodifiableSet(new LocalStoreKeySet(localStore.getKeySet(), localStore.size(), IGNORE_ID_FILTER));
  }

  /**
   * Returned value may be coherent or incoherent or null
   */
  public AbstractLocalCacheStoreValue getLocalValue(final Object key) {
    AbstractLocalCacheStoreValue value = this.localStore.get(key);
    if (value != null && value.isIncoherentValue() && value.isIncoherentTooLong()) {
      // if incoherent and been incoherent too long, remove from cache/map
      this.localStore.remove(key, RemoveType.NORMAL);
      return null;
    }
    return value;
  }

  /**
   * Returned value is always coherent or null.
   */
  public AbstractLocalCacheStoreValue getCoherentLocalValue(final Object key) {
    final AbstractLocalCacheStoreValue value = getLocalValue(key);
    if (value != null && value.isIncoherentValue()) {
      // don't return incoherent items from here
      this.localStore.remove(key, RemoveType.NORMAL);
      return null;
    }
    return value;
  }

  public void unpinEntry(Object key) {
    this.localStore.unpinEntry(key);
  }

  public void removeEntriesForObjectId(ObjectID objectId) {
    removeEntriesForId(objectId);
  }

  public void removeEntriesForLockId(LockID lockId) {
    removeEntriesForId(lockId);
  }

  private void removeEntriesForId(Object id) {
    if (!isStoreInitialized()) { return; }

    // This should be called when a lock has already been recalled, so shouldn't be a problem
    executeUnderSegmentWriteLock(id, null, RemoveEntriesForIdCallback.INSTANCE);
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  public void addAllObjectIDsToValidate(Map map) {
    if (!isStoreInitialized()) { return; }

    for (ReentrantReadWriteLock readWriteLock : segmentLocks) {
      readWriteLock.readLock().lock();
    }
    try {
      if (this.localStore.size() != 0) {
        ObjectIDSet set = new ObjectIDSet();

        Set currentSet = this.localStore.getKeySet();
        if (currentSet != null) {
          for (Object id : currentSet) {
            // TODO: keys added from serverMapLocalCache can never be ObjectID, need other special handling here?
            if (id instanceof ObjectID && id != ObjectID.NULL_ID) {
              set.add((ObjectID) id);
            }
          }
        }

        if (!set.isEmpty()) {
          map.put(mapID, set);
        }
      }
    } finally {
      for (ReentrantReadWriteLock readWriteLock : segmentLocks) {
        readWriteLock.readLock().unlock();
      }
    }
  }

  private void initiateLockRecall(LockID id) {
    if (id != null) {
      Set<LockID> lockID = Collections.singleton(id);
      globalLocalCacheManager.initiateLockRecall(lockID);
    }
  }

  private void initiateLockRecall(Set<LockID> ids) {
    globalLocalCacheManager.initiateLockRecall(ids);
  }

  private void initiateInlineLockRecall(Set<LockID> ids) {
    globalLocalCacheManager.recallLocksInline(ids);
  }

  public void evictExpired(Object key, AbstractLocalCacheStoreValue value) {
    final TCServerMap serverMap = (TCServerMap) tcObjectServerMap.getPeerObject();

    if (serverMap != null && localStore.get(key) == null) {
      serverMap.evictExpired(key, value.getValue());
    }
  }

  /**
   * used for tests
   */
  L1ServerMapLocalCacheStore getL1ServerMapLocalCacheStore() {
    return localStore;
  }

  private Set<LockID> executeUnderMapWriteLock(ClearAllEntriesCallback callback) {
    for (ReentrantReadWriteLock readWriteLock : segmentLocks) {
      readWriteLock.writeLock().lock();
    }
    try {
      return callback.callback(null, null, this.localStore);
    } finally {
      for (ReentrantReadWriteLock readWriteLock : segmentLocks) {
        readWriteLock.writeLock().unlock();
      }
    }
  }

  private <V> V executeUnderSegmentWriteLock(final Object id, final Object key,
                                             final ExecuteUnderLockCallback<V> callback) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      return callback.callback(id, key, this.localStore);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static interface ExecuteUnderLockCallback<V> {
    V callback(Object key, Object value, L1ServerMapLocalCacheStore backingMap);
  }

  private static class AddIdKeyMappingCallback implements ExecuteUnderLockCallback<Void> {

    public static AddIdKeyMappingCallback INSTANCE = new AddIdKeyMappingCallback();

    public Void callback(Object id, Object key, L1ServerMapLocalCacheStore backingMap) {
      List list;
      synchronized (id) {
        list = (List) backingMap.get(id);
        if (list == null) {
          list = new ArrayList();
          // TODO: use putIfAbsent
          backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
        }
      }
      list.add(key);

      // add the list back
      backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
      return null;
    }
  }

  private static class RemoveIdKeyMappingCallback implements ExecuteUnderLockCallback<LockID> {
    public static RemoveIdKeyMappingCallback INSTANCE = new RemoveIdKeyMappingCallback();

    public LockID callback(Object id, Object key, L1ServerMapLocalCacheStore backingMap) {
      List list = (List) backingMap.get(id);
      if (list == null) { return null; }
      list.remove(key);

      // put back or remove the list
      if (list.size() == 0) {
        backingMap.remove(id, RemoveType.NO_SIZE_DECREMENT);
      } else {
        backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
      }
      return list.size() == 0 && (id instanceof LockID) ? (LockID) id : null;
    }
  }

  private static class RemoveEntriesForIdCallback implements ExecuteUnderLockCallback<Void> {
    public static RemoveEntriesForIdCallback INSTANCE = new RemoveEntriesForIdCallback();

    public Void callback(Object id, Object unusedParam, L1ServerMapLocalCacheStore backingMap) {
      // remove the list
      List list = (List) backingMap.remove(id, RemoveType.NO_SIZE_DECREMENT);
      if (list != null) {
        for (Object key : list) {
          // remove each key from the backing map/store
          backingMap.remove(key, RemoveType.NORMAL);
        }
      }
      return null;
    }
  }

  private static class RemoveEntryForKeyCallback implements ExecuteUnderLockCallback<LockID> {
    public static RemoveEntryForKeyCallback INSTANCE = new RemoveEntryForKeyCallback();

    public LockID callback(Object id, Object key, L1ServerMapLocalCacheStore backingMap) {
      List list = (List) backingMap.get(id);
      if (list != null) {
        // remove the key from the id->list(keys)
        list.remove(key);
        // remove the key from the backing map/store
        backingMap.remove(key, RemoveType.NORMAL);

        // put back or remove the list
        if (list.size() == 0) {
          backingMap.remove(id, RemoveType.NO_SIZE_DECREMENT);
        } else {
          backingMap.put(id, list, PutType.PINNED_NO_SIZE_INCREMENT);
        }
      }
      return list != null && list.size() == 0 && (id instanceof LockID) ? (LockID) id : null;
    }
  }

  private static class ClearAllEntriesCallback implements ExecuteUnderLockCallback<Set<LockID>> {
    public static ClearAllEntriesCallback INSTANCE = new ClearAllEntriesCallback();

    public Set<LockID> callback(Object unused1, Object unused2, L1ServerMapLocalCacheStore backingMap) {
      HashSet<LockID> lockIDs = new HashSet<LockID>();
      Set keySet = backingMap.getKeySet();
      for (Object key : keySet) {
        if (key instanceof LockID) {
          lockIDs.add((LockID) key);
        }
      }

      backingMap.clear();
      return lockIDs;
    }
  }

  static class IgnoreIdsFilter implements LocalStoreKeySetFilter {

    public boolean accept(Object value) {
      if (value instanceof ObjectID || value instanceof LockID) { return false; }
      return true;
    }

  }
}
