/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

/**
 * Used to be cached item store
 */
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallHelper;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.ServerMapLocalCacheIdStore;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMapLocalCacheIdStoreImpl implements ServerMapLocalCacheIdStore {
  private static final TCLogger          LOGGER       = TCLogging.getLogger(ServerMapLocalCacheIdStoreImpl.class);
  private final static int               CONCURRENCY  = 128;

  private final ReentrantReadWriteLock[] segmentLocks = new ReentrantReadWriteLock[CONCURRENCY];
  private final BackingMap               backingMap   = new BackingMap();
  private final LocksRecallHelper        locksRecallHelper;

  public ServerMapLocalCacheIdStoreImpl(LocksRecallHelper locksRecallHelper) {
    this.locksRecallHelper = locksRecallHelper;
    for (int i = 0; i < segmentLocks.length; i++) {
      this.segmentLocks[i] = new ReentrantReadWriteLock();
    }
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore localCacheStore) {
    backingMap.setupLocalStore(localCacheStore);
  }

  // Used in tests
  List get(final Object id) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.readLock().lock();
      Object obj = this.backingMap.get(id);
      if (obj instanceof List) {
        return (List) obj;
      } else {
        LOGGER.warn("Get for: '" + id + "': not mapped to a list - " + obj);
        return null;
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Used only in handshake
   */
  public void addAllObjectIDsToValidate(ObjectID mapID, Map map) {
    for (ReentrantReadWriteLock readWriteLock : segmentLocks) {
      readWriteLock.readLock().lock();
    }
    try {
      if (this.backingMap.size() != 0) {
        ObjectIDSet set = new ObjectIDSet();

        Set currentSet = this.backingMap.getKeySet();
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

  private ReentrantReadWriteLock getLock(Object key) {
    int index = Math.abs(key.hashCode() % CONCURRENCY);
    return segmentLocks[index];
  }

  private Set<LockID> executeUnderMapWriteLock(ClearAllEntriesCallback callback) {
    for (ReentrantReadWriteLock readWriteLock : segmentLocks) {
      readWriteLock.writeLock().lock();
    }
    try {
      return callback.callback(null, null, backingMap);
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
      return callback.callback(id, key, backingMap);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void addIdKeyMapping(final Object id, final Object key) {
    executeUnderSegmentWriteLock(id, key, AddIdKeyMappingCallback.INSTANCE);
  }

  public void removeIdKeyMapping(final Object id, final Object key) {
    LockID lockID = executeUnderSegmentWriteLock(id, key, RemoveIdKeyMappingCallback.INSTANCE);
    initiateLockRecall(lockID);
  }

  public void removeEntries(Object id) {
    // This should be called when a lock has already been recalled, so shouldn't be a problem
    executeUnderSegmentWriteLock(id, null, RemoveEntriesForIdCallback.INSTANCE);
  }

  public void evictedFromStore(Object id, Object key) {
    LockID lockID = executeUnderSegmentWriteLock(id, key, RemoveEntryForKeyCallback.INSTANCE);
    initiateLockRecall(lockID);
  }

  public void removeEntryForKey(Object key) {
    Object value = backingMap.remove(key);
    if (value != null && value instanceof AbstractLocalCacheStoreValue) {
      AbstractLocalCacheStoreValue localValue = (AbstractLocalCacheStoreValue) value;
      if (localValue.getId() != null) {
        // not incoherent item, remove id-key mapping
        LockID id = executeUnderSegmentWriteLock(localValue.getId(), key, RemoveEntryForKeyCallback.INSTANCE);
        initiateLockRecall(id);
        // TODO: should we do a recall/unpin when list size becomes 0 OR unpin the lock
      }
    }
  }

  public void clearAllEntries() {
    Set<LockID> lockIDs = executeUnderMapWriteLock(ClearAllEntriesCallback.INSTANCE);
    // TODO some places we need to do this inline, will handle that later
    initiateLockRecall(lockIDs);
  }

  private static class BackingMap {
    private volatile L1ServerMapLocalCacheStore store;

    public void setupLocalStore(L1ServerMapLocalCacheStore localCacheStore) {
      this.store = localCacheStore;
    }

    private boolean isStoreInitialized() {
      if (store == null) {
        LOGGER.info("Store is not setup yet");
        return false;
      }
      return true;
    }

    Object get(Object key) {
      if (isStoreInitialized()) {
        return store.get(key);
      } else {
        return null;
      }
    }

    void putPinnedEntry(Object key, List value) {
      if (isStoreInitialized()) {
        store.putPinnedEntry(key, value);
      }
    }

    Object remove(Object key) {
      if (isStoreInitialized()) {
        return store.remove(key);
      } else {
        return null;
      }
    }

    int size() {
      if (isStoreInitialized()) {
        return store.size();
      } else {
        return 0;
      }
    }

    Set getKeySet() {
      if (isStoreInitialized()) {
        return store.getKeySet();
      } else {
        return null;
      }
    }

    public void clear() {
      if (isStoreInitialized()) {
        store.clear();
      }
    }

  }

  private static interface ExecuteUnderLockCallback<V> {
    V callback(Object key, Object value, BackingMap backingMap);
  }

  private static class AddIdKeyMappingCallback implements ExecuteUnderLockCallback<Void> {

    public static AddIdKeyMappingCallback INSTANCE = new AddIdKeyMappingCallback();

    public Void callback(Object id, Object key, BackingMap backingMap) {
      List list = (List) backingMap.get(id);
      if (list == null) {
        list = new ArrayList();
        backingMap.putPinnedEntry(id, list);
      }
      list.add(key);

      // add the list back
      backingMap.putPinnedEntry(id, list);
      return null;
    }
  }

  private static class RemoveIdKeyMappingCallback implements ExecuteUnderLockCallback<LockID> {
    public static RemoveIdKeyMappingCallback INSTANCE = new RemoveIdKeyMappingCallback();

    public LockID callback(Object id, Object key, BackingMap backingMap) {
      List list = (List) backingMap.get(id);
      if (list == null) { return null; }
      list.remove(key);

      // put back or remove the list
      if (list.size() == 0) {
        backingMap.remove(id);
        // TODO: do we need recall/unpin here?
      } else {
        backingMap.putPinnedEntry(id, list);
      }
      return list.size() == 0 && (id instanceof LockID) ? (LockID) id : null;
    }
  }

  private static class RemoveEntriesForIdCallback implements ExecuteUnderLockCallback<Void> {
    public static RemoveEntriesForIdCallback INSTANCE = new RemoveEntriesForIdCallback();

    public Void callback(Object id, Object unusedParam, BackingMap backingMap) {
      // remove the list
      List list = (List) backingMap.remove(id);
      if (list != null) {
        for (Object key : list) {
          // remove each key from the backing map/store
          backingMap.remove(key);
        }
      }
      return null;
    }
  }

  private static class RemoveEntryForKeyCallback implements ExecuteUnderLockCallback<LockID> {
    public static RemoveEntryForKeyCallback INSTANCE = new RemoveEntryForKeyCallback();

    public LockID callback(Object id, Object key, BackingMap backingMap) {
      List list = (List) backingMap.get(id);
      if (list != null) {
        // remove the key from the id->list(keys)
        list.remove(key);
        // remove the key from the backing map/store
        backingMap.remove(key);

        // put back or remove the list
        if (list.size() == 0) {
          backingMap.remove(id);
        } else {
          backingMap.putPinnedEntry(id, list);
        }
      }
      return list != null && list.size() == 0 && (id instanceof LockID) ? (LockID) id : null;
    }
  }

  private static class ClearAllEntriesCallback implements ExecuteUnderLockCallback<Set<LockID>> {
    public static ClearAllEntriesCallback INSTANCE = new ClearAllEntriesCallback();

    public Set<LockID> callback(Object unused1, Object unused2, BackingMap backingMap) {
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

  private void initiateLockRecall(LockID id) {
    if (id != null) {
      Set<LockID> lockID = Collections.singleton(id);
      locksRecallHelper.initiateLockRecall(lockID);
    }
  }

  private void initiateLockRecall(Set<LockID> ids) {
    locksRecallHelper.initiateLockRecall(ids);
  }
}
