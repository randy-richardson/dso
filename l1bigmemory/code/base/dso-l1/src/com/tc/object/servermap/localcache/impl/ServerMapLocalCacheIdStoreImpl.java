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
  private static final TCLogger               LOGGER       = TCLogging.getLogger(ServerMapLocalCacheIdStoreImpl.class);
  private final static int                    CONCURRENCY  = 128;

  private final ReentrantReadWriteLock[]      segmentLocks = new ReentrantReadWriteLock[CONCURRENCY];
  private volatile L1ServerMapLocalCacheStore backingMap;
  private final LocksRecallHelper             locksRecallHelper;

  public ServerMapLocalCacheIdStoreImpl(LocksRecallHelper locksRecallHelper) {
    this.locksRecallHelper = locksRecallHelper;
    for (int i = 0; i < segmentLocks.length; i++) {
      this.segmentLocks[i] = new ReentrantReadWriteLock();
    }
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore localCacheStore) {
    backingMap = localCacheStore;
  }

  private boolean isStoreInitialized() {
    if (backingMap == null) {
      LOGGER.warn("Store yet not initialized");
      return false;
    }
    return true;
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
    if (!isStoreInitialized()) { return; }

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
    if (!isStoreInitialized()) { return; }
    executeUnderSegmentWriteLock(id, key, AddIdKeyMappingCallback.INSTANCE);
  }

  public void removeIdKeyMapping(final Object id, final Object key) {
    if (!isStoreInitialized()) { return; }

    LockID lockID = executeUnderSegmentWriteLock(id, key, RemoveIdKeyMappingCallback.INSTANCE);
    initiateLockRecall(lockID);
  }

  public void removeEntries(Object id) {
    if (!isStoreInitialized()) { return; }

    // This should be called when a lock has already been recalled, so shouldn't be a problem
    executeUnderSegmentWriteLock(id, null, RemoveEntriesForIdCallback.INSTANCE);
  }

  public void evictedFromStore(Object id, Object key) {
    if (!isStoreInitialized()) { return; }

    LockID lockID = executeUnderSegmentWriteLock(id, key, RemoveEntryForKeyCallback.INSTANCE);
    initiateLockRecall(lockID);
  }

  public void removeEntryForKey(Object key) {
    if (!isStoreInitialized()) { return; }

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
    if (!isStoreInitialized()) { return; }

    Set<LockID> lockIDs = executeUnderMapWriteLock(ClearAllEntriesCallback.INSTANCE);
    // TODO some places we need to do this inline, will handle that later
    initiateLockRecall(lockIDs);
  }

  private static interface ExecuteUnderLockCallback<V> {
    V callback(Object key, Object value, L1ServerMapLocalCacheStore backingMap);
  }

  private static class AddIdKeyMappingCallback implements ExecuteUnderLockCallback<Void> {

    public static AddIdKeyMappingCallback INSTANCE = new AddIdKeyMappingCallback();

    public Void callback(Object id, Object key, L1ServerMapLocalCacheStore backingMap) {
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

    public LockID callback(Object id, Object key, L1ServerMapLocalCacheStore backingMap) {
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

    public Void callback(Object id, Object unusedParam, L1ServerMapLocalCacheStore backingMap) {
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

    public LockID callback(Object id, Object key, L1ServerMapLocalCacheStore backingMap) {
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
