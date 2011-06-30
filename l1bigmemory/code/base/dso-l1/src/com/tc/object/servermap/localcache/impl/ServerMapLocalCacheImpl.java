/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.GlobalLocalCacheManager;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.impl.L1ServerMapLocalStoreTransactionCompletionListener.TransactionCompleteOperation;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ServerMapLocalCacheImpl implements ServerMapLocalCache {
  private final ServerMapLocalCacheIDStore                                               cacheIDStore = new ServerMapLocalCacheIDStore();
  private final L1ServerMapLocalCacheStoreListener<Object, AbstractLocalCacheStoreValue> localStoreEvictionListener;
  private final ObjectID                                                                 mapID;
  private final GlobalLocalCacheManager                                                  globalLocalCacheManager;
  private final boolean                                                                  localCacheEnabled;
  private volatile L1ServerMapLocalCacheStore<Object, AbstractLocalCacheStoreValue>      localStore;
  private final ClientObjectManager                                                      objectManager;
  private final Manager                                                                  manager;

  /**
   * Not public constructor, should be created only by the global local cache manager
   */
  ServerMapLocalCacheImpl(ObjectID mapID, ClientObjectManager objectManager, Manager manager,
                          GlobalLocalCacheManager globalLocalCacheManager, boolean islocalCacheEnbaled) {
    this.mapID = mapID;
    this.objectManager = objectManager;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.localCacheEnabled = islocalCacheEnbaled;
    this.localStoreEvictionListener = new L1ServerMapLocalCacheStoreListenerImpl(this);
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore store) {
    this.localStore = store;
    this.cacheIDStore.setupLocalStore(store);
    this.localStore.addListener(localStoreEvictionListener);
  }

  public void addStrongValueToCache(LockID lockId, Object key, Object value, MapOperationType mapOperation) {
    final LocalCacheStoreStrongValue localCacheValue = new LocalCacheStoreStrongValue(lockId, value);
    addToCache(key, localCacheValue, mapOperation);
  }

  public void addEventualValueToCache(ObjectID valueObjectId, Object key, Object value, MapOperationType mapOperation) {
    final LocalCacheStoreEventualValue localCacheValue = new LocalCacheStoreEventualValue(valueObjectId, value);
    addToCache(key, localCacheValue, mapOperation);
  }

  public void addIncoherentValueToCache(Object key, Object value, MapOperationType mapOperation) {
    final LocalCacheStoreIncoherentValue localCacheValue = new LocalCacheStoreIncoherentValue(value);
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
        old = this.localStore.putPinnedEntry(key, localCacheValue);
      } else {
        old = this.localStore.put(key, localCacheValue);
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
    if (localCacheValue != null) {
      if (localCacheValue.isStrongConsistentValue() || localCacheValue.isEventualConsistentValue()) {
        if (localCacheValue.getId() != null && localCacheValue.getId() != ObjectID.NULL_ID) {
          cacheIDStore.remove(localCacheValue.getId(), key);
        } // else: we don't add for null_id
      } // else: no need to remove for incoherent items
    }
  }

  private void addIdToKeysMappingIfNecessary(final AbstractLocalCacheStoreValue localCacheValue, final Object key) {
    if (localCacheValue.isStrongConsistentValue() || localCacheValue.isEventualConsistentValue()) {
      // remember the key for this id, used on recall of lock or invalidation
      // for strong and eventual values, multiple keys can be mapped to same single id
      if (localCacheValue.getId() != null && localCacheValue.getId() != ObjectID.NULL_ID) {
        // we don't add for null_id
        cacheIDStore.add(localCacheValue.getId(), key);
      }
    } // else: incoherent items no need to be remembered
  }

  private L1ServerMapLocalStoreTransactionCompletionListener getTransactionCompleteListener(final Object key,
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

  public void clearAllLocalCache() {
    // TODO: need to clear id store too?
    this.localStore.clear();
  }

  public int size() {
    // TODO: need to handle to ignore meta entries (id -> List<keys>)
    return this.localStore.size();
  }

  public int evictCachedEntries(int toClear) {
    return this.localStore.evict(toClear);
  }

  /**
   * When a remove from local cache is called, remove and flush
   */
  public void removeFromLocalCache(Object key) {
    AbstractLocalCacheStoreValue value = this.localStore.remove(key);
    // TODO: need to remove from id store?
    if (value != null) {
      localStoreEvictionListener.notifyElementEvicted(key, value);
    }
  }

  public Set getKeySet() {
    // TODO: need to handle to ignore meta entries (id -> List<keys>)
    return this.localStore.getKeySet();
  }

  // TODO: need to make sure in RemoteServerMapManager that when no CachedItems are 0 for a particular lockid, i need
  // to recall the lock<br>
  // Also do we even need this especially when we have removeFromLocalCache
  public void evictFromLocalCache(Object key, AbstractLocalCacheStoreValue ci) {
    // TODO: merge/delegate to removeFromLocalCache?
    AbstractLocalCacheStoreValue value = this.localStore.remove(key);
    removeIdToKeysMappingIfNecessary(value, key);
  }

  /**
   * Returned value may be coherent or incoherent or null
   */
  public AbstractLocalCacheStoreValue getLocalValue(final Object key) {
    AbstractLocalCacheStoreValue value = this.localStore.get(key);
    if (value != null && value.isIncoherentValue() && value.isIncoherentTooLong()) {
      // if incoherent and been incoherent too long, remove from cache/map
      this.localStore.remove(key);
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
      this.localStore.remove(key);
      return null;
    }
    return value;
  }

  public void unpinEntry(Object key) {
    this.localStore.unpinEntry(key);
  }

  public void clearForIDsAndRecallLocks(Set<LockID> evictedLockIds) {
    globalLocalCacheManager.recallLocks(evictedLockIds);
  }

  public void removeEntriesForObjectId(ObjectID objectId) {
    removeEntriesForId(objectId);
  }

  public void removeEntriesForLockId(LockID lockId) {
    removeEntriesForId(lockId);
  }

  /**
   * When a flush is called here it means do this:<br>
   * 1) Remove from ID Store i.e. store flush <br>
   * 2) Remove keys obtained from ID Store<br>
   * This method wont call in to recall locks, hence if u want to recall locks call clearCachedItemsForLocks
   */
  private void removeEntriesForId(Object id) {
    List keys = cacheIDStore.remove(id);
    if (keys == null) {
      // This can happen when remove is called due an "remove" from local cache on a remove from CDSM
      return;
    }

    for (Object key : keys) {
      this.localStore.remove(key);
    }
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  public void addAllObjectIDsToValidate(Map tempMap) {
    this.cacheIDStore.addAllObjectIDsToValidate(mapID, tempMap);
  }

  ServerMapLocalCacheIDStore getCacheIDStore() {
    return cacheIDStore;
  }
}
