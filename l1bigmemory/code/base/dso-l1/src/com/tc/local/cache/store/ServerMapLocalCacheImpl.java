/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.UnlockedSharedObjectException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ServerMapLocalCacheImpl implements ServerMapLocalCache {
  private final ServerMapLocalCacheIDStore                                       cacheIDStore = new ServerMapLocalCacheIDStore();
  private final L1ServerMapLocalCacheStoreListener<Object, LocalCacheStoreValue> localStoreEvictionListener;
  private final ObjectID                                                         mapID;
  private final GlobalLocalCacheManager                                          globalLocalCacheManager;
  private final boolean                                                          localCacheEnabled;
  private volatile L1ServerMapLocalCacheStore<Object, LocalCacheStoreValue>      localStore;
  private final ClientObjectManager                                              objectManager;
  private final Manager                                                          manager;

  public ServerMapLocalCacheImpl(ObjectID mapID, ClientObjectManager objectManager, Manager manager,
                                 GlobalLocalCacheManager globalLocalCacheManager, boolean islocalCacheEnbaled) {
    this.mapID = mapID;
    this.objectManager = objectManager;
    this.manager = manager;
    this.globalLocalCacheManager = globalLocalCacheManager;
    this.localCacheEnabled = islocalCacheEnbaled;
    this.globalLocalCacheManager.addLocalCache(this.mapID, this);
    this.localStoreEvictionListener = new L1ServerMapLocalCacheStoreListenerImpl(this);
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore store) {
    this.localStore = store;
    this.cacheIDStore.setupLocalStore(store);
    this.localStore.addListener(localStoreEvictionListener);
  }

  /**
   * Creates a coherent mapping for the key to CachedItem<br>
   * TODO: make sure remove actually removes from local cache after transction completes
   * 
   * @param id - LockID that is protecting the item
   * @param key - key of the mapping
   * @param value - value of the mapping
   * @param b
   */
  public void addCoherentValueToCache(final Object id, final Object key, final Object value, boolean isMutate) {
    addCoherentValueToCache(id, key, value, isMutate, false);
  }

  public void addCoherentValueToCache(final Object id, final Object key, final Object value, boolean isMutate,
                                      boolean isRemove) {
    final LocalCacheStoreValue item;
    final L1ServerMapLocalStoreTransactionCompletionListener txnCompleteListener;
    if (!localCacheEnabled) {
      txnCompleteListener = new L1ServerMapLocalStoreTransactionCompletionListener(this, key, true);
      item = new LocalCacheStoreValue(id, value);
    } else {
      txnCompleteListener = isMutate ? new L1ServerMapLocalStoreTransactionCompletionListener(this, key, isRemove)
          : null;
      item = new LocalCacheStoreValue(id, value);
    }
    addToCache(key, item, isMutate, txnCompleteListener);
  }

  private void registerForCallbackOnComplete(final L1ServerMapLocalStoreTransactionCompletionListener l1ServerMapLocalStoreTransactionCompletionListener) {
    if (l1ServerMapLocalStoreTransactionCompletionListener == null) { return; }
    ClientTransaction txn = this.objectManager.getTransactionManager().getCurrentTransaction();
    if (txn == null) { throw new UnlockedSharedObjectException(
                                                               "Attempt to access a shared object outside the scope of a shared lock.",
                                                               Thread.currentThread().getName(), manager.getClientID()); }
    txn.addTransactionCompleteListener(l1ServerMapLocalStoreTransactionCompletionListener);
  }

  public void addIncoherentValueToCache(final Object key, final Object value, boolean isMutate) {
    final LocalCacheStoreValue item;
    final L1ServerMapLocalStoreTransactionCompletionListener txnCompleteListener;
    if (!localCacheEnabled) {
      txnCompleteListener = new L1ServerMapLocalStoreTransactionCompletionListener(this, key, true);
      item = new LocalCacheStoreValue(null, value, true);
    } else {
      txnCompleteListener = isMutate ? new L1ServerMapLocalStoreTransactionCompletionListener(this, key, false) : null;
      item = new LocalCacheStoreValue(null, value, true);
    }
    addToCache(key, item, isMutate, txnCompleteListener);
  }

  // TODO::FIXME:: There is a race for puts for same key from same vm - it races between the map.put() and
  // serverMapManager.put()
  private void addToCache(final Object key, final LocalCacheStoreValue item, boolean isMutate,
                          L1ServerMapLocalStoreTransactionCompletionListener txnCompleteListener) {
    if (!localCacheEnabled && !isMutate) {
      // local cache NOT enabled AND NOT a mutate operation, do not cache anything locally
      // for mutate ops keep in local cache till txn is complete
      return;
    }
    final LocalCacheStoreValue old = this.localStore.put(key, item);
    if (old != null) {
      Object oldID = old.getID();
      // TODO: do we need a null id check, may be when we remove we do put a NULL_ID
      if (oldID != null && oldID != ObjectID.NULL_ID) {
        // I think this should be removeCachedItem only
        cacheIDStore.remove(oldID, key);
      }
    }
    Object itemID = item.getID();
    if (itemID != null && itemID != ObjectID.NULL_ID) {
      cacheIDStore.add(itemID, key);
    }
    if (isMutate) {
      registerForCallbackOnComplete(txnCompleteListener);
    }
  }

  // private boolean isExpired(final CachedItem ci, final int now) {
  // final ExpirableEntry ee;
  // if ((TCObjectServerMapImpl.this.tti <= 0 && TCObjectServerMapImpl.this.ttl <= 0)
  // || (ee = ci.getExpirableEntry()) == null) { return false; }
  // return now >= ee.expiresAt(TCObjectServerMapImpl.this.tti, TCObjectServerMapImpl.this.ttl);
  // }

  public void clearAllLocalCache() {
    this.localStore.clear();
  }

  public int size() {
    return this.localStore.size();
  }

  public int evictCachedEntries(int toClear) {
    return this.localStore.evict(toClear);
  }

  /**
   * When a remove from local cache is called, remove and flush
   */
  public void removeFromLocalCache(Object key) {
    LocalCacheStoreValue value = this.localStore.remove(key);
    if (value != null) {
      localStoreEvictionListener.notifyElementEvicted(key, value);
    }
  }

  public Set getKeySet() {
    return this.localStore.getKeySet();
  }

  // TODO: need to make sure in RemoteServerMapManager that when no CachedItems are 0 for a particular lockid, i need
  // to recall the lock<br>
  // Also do we even need this especially when we have removeFromLocalCache
  public void evictFromLocalCache(Object key, LocalCacheStoreValue ci) {
    LocalCacheStoreValue value = this.localStore.remove(key);
    if (value != null) {
      cacheIDStore.remove(value.getID(), key);
    }
  }

  /**
   * Returned value may be coherent or incoherent or null
   */
  public LocalCacheStoreValue getLocalValue(final Object key) {
    LocalCacheStoreValue value = this.localStore.get(key);
    if (value != null && value.isIncoherent() && value.isIncoherentTooLong()) {
      // if incoherent and been incoherent too long, remove from cache/map
      this.localStore.remove(key);
      return null;
    }
    return value;
  }

  /**
   * Returned value is always coherent or null.
   */
  public LocalCacheStoreValue getCoherentLocalValue(final Object key) {
    final LocalCacheStoreValue value = getLocalValue(key);
    if (value != null && value.isIncoherent()) {
      this.localStore.remove(key);
      return null;
    }
    return value;
  }

  public void pinEntry(Object key) {
    this.localStore.pinEntry(key);
  }

  public void unpinEntry(Object key) {
    this.localStore.unpinEntry(key);
  }

  public void clearForIDsAndRecallLocks(Set<LockID> evictedLockIds) {
    for (LockID lockID : evictedLockIds) {
      flush(lockID);
    }

    globalLocalCacheManager.recallLocks(evictedLockIds);
  }

  /**
   * When a flush is called here it means do this:<br>
   * 1) Remove from ID Store i.e. store flush <br>
   * 2) Remove keys obtained from ID Store<br>
   * This method wont call in to recall locks, hence if u want to recall locks call clearCachedItemsForLocks
   */
  public void flush(Object id) {
    List keys = cacheIDStore.remove(id);
    if (keys == null) {
      // This can happen when remove is called due an "remove" from local cache on a remove frm CDSM
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
