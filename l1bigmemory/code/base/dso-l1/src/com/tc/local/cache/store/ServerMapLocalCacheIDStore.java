/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

/**
 * Used to be cached item store
 */

import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerMapLocalCacheIDStore<L> {
  private final static int               CONCURRENCY    = 1;
  private final ReentrantReadWriteLock[] readWriteLocks = new ReentrantReadWriteLock[CONCURRENCY];

  // TODO: This should be an ehcache later
  private final Map<L, List>             store;

  public ServerMapLocalCacheIDStore() {
    this.store = new HashMap<L, List>();
    for (int i = 0; i < readWriteLocks.length; i++) {
      this.readWriteLocks[i] = new ReentrantReadWriteLock();
    }
  }

  // For tests
  List get(final L id) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.readLock().lock();
      return this.store.get(id);
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
    List list = this.store.get(id);
    if (list == null) {
      list = new ArrayList();
      this.store.put(id, list);
    }

    list.add(key);
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
    List list = this.store.get(id);
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
      this.store.remove(id);
    }
  }

  public List remove(final L id) {
    final List list;
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      list = this.store.remove(id);
    } finally {
      lock.writeLock().unlock();
    }
    return list;
  }

  /**
   * Used only in handshake
   */
  public void addAllObjectIDsToValidate(ObjectID mapID, Map map) {
    for (ReentrantReadWriteLock readWriteLock : readWriteLocks) {
      readWriteLock.readLock().lock();
    }
    try {
      if (!this.store.isEmpty()) {
        ObjectIDSet set = new ObjectIDSet();
        Set currentSet = this.store.keySet();
        for (Object id : currentSet) {
          if (id instanceof ObjectID) {
            set.add((ObjectID) id);
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
    return readWriteLocks[key.hashCode()];
  }
}
