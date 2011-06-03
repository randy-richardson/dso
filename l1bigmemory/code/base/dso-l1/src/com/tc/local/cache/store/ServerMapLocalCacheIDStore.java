/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

/**
 * Used to be cached item store
 */
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMapLocalCacheIDStore<L> {
  private static final TCLogger          LOGGER         = TCLogging.getLogger(ServerMapLocalCacheIDStore.class);
  private final static int               CONCURRENCY    = 1;

  private final ReentrantReadWriteLock[] readWriteLocks = new ReentrantReadWriteLock[CONCURRENCY];

  private final BackingMap               backingMap     = new BackingMap();

  public ServerMapLocalCacheIDStore() {
    for (int i = 0; i < readWriteLocks.length; i++) {
      this.readWriteLocks[i] = new ReentrantReadWriteLock();
    }
  }

  public void setupLocalStore(L1ServerMapLocalCacheStore localCacheStore) {
    backingMap.setupLocalStore(localCacheStore);
  }

  // Used in tests
  int size() {
    // TODO
    return 0;
  }

  // Used in tests
  List get(final L id) {
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

  public void add(final L id, final Object key) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      addInternal(id, key);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void addInternal(L id, Object key) {
    List list = (List) backingMap.get(id);
    if (list == null) {
      list = new ArrayList();
      this.backingMap.put(id, list);
      // TODO: need to pin this element?
    }
    list.add(key);
    // TODO: need to put back the list in the store?
  }

  public void remove(final L id, final Object key) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      removeInternal(id, key);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // TODO: should we do a recall when list size becomes 0 OR unpin the lock
  private void removeInternal(L id, Object key) {
    List list = get(id);
    if (list == null) { return; }

    Iterator iterator = list.iterator();
    while (iterator.hasNext()) {
      Object tempKey = iterator.next();
      if (tempKey.equals(key)) {
        iterator.remove();
        break;
      }
    }

    if (list.size() == 0) {
      this.backingMap.remove(id);
    }
  }

  public List remove(final L id) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      final Object obj = this.backingMap.remove(id);
      if (obj instanceof List) {
        return (List) obj;
      } else {
        LOGGER.warn("Remove for: '" + id + "' is not mapped to a list - " + obj);
        return null;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Used only in handshake
   */
  public void addAllObjectIDsToValidate(ObjectID mapID, Map map) {
    for (ReentrantReadWriteLock readWriteLock : readWriteLocks) {
      readWriteLock.readLock().lock();
    }
    try {
      if (this.backingMap.size() != 0) {
        ObjectIDSet set = new ObjectIDSet();

        Set currentSet = this.backingMap.getKeySet();
        if (currentSet != null) {
          for (Object id : currentSet) {
            // TODO: keys added from serverMapLocalCache can never be ObjectID, need other special handling here?
            if (id instanceof ObjectID) {
              set.add((ObjectID) id);
            }
          }
        }

        if (!set.isEmpty()) {
          map.put(mapID, set);
        }
      }
    } finally {
      for (ReentrantReadWriteLock readWriteLock : readWriteLocks) {
        readWriteLock.readLock().unlock();
      }
    }
  }

  private ReentrantReadWriteLock getLock(L key) {
    int index = Math.abs(key.hashCode() % CONCURRENCY);
    return readWriteLocks[index];
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

    void put(Object key, List value) {
      if (isStoreInitialized()) {
        store.put(key, value);
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

  }
}
