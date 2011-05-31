/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.cache;

import com.tc.object.ObjectID;
import com.tc.object.RemoteServerMapManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CachedItemStore<L> {
  private final static int               CONCURRENCY    = 1024;

  private final ReentrantReadWriteLock[] readWriteLocks = new ReentrantReadWriteLock[CONCURRENCY];

  // TODO: This should be an ehcache later
  private final Map<L, List<KeyMapID>>   store;

  private final RemoteServerMapManager   remoteServerMapManager;

  /**
   * Creates a CachedItemStore with the specified initial capacity, load factor and concurrency level.
   */
  public CachedItemStore(RemoteServerMapManager remoteServerMapManager) {
    this.store = new HashMap<L, List<KeyMapID>>();
    for (int i = 0; i < readWriteLocks.length; i++) {
      this.readWriteLocks[i] = new ReentrantReadWriteLock();
    }
    this.remoteServerMapManager = remoteServerMapManager;
  }

  // For tests
  List<KeyMapID> get(final L id) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.readLock().lock();
      return this.store.get(id);
    } finally {
      lock.readLock().unlock();
    }
  }

  public void add(final L id, final Object key, final ObjectID mapID) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      addInternal(id, key, mapID);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void addInternal(L id, Object key, ObjectID mapID) {
    KeyMapID keyMapID = new KeyMapID(key, mapID);
    List<KeyMapID> list = this.store.get(id);
    if (list == null) {
      list = new ArrayList<KeyMapID>();
      this.store.put(id, list);
    }

    list.add(keyMapID);
  }

  public void remove(final L id, final Object key, final ObjectID mapID) {
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      removeInternal(id, key, mapID);
    } finally {
      lock.writeLock().unlock();
    }
  }

  // TODO: should we do a recall when list size becomes 0 OR unpin the lock
  private void removeInternal(L id, Object key, ObjectID mapID) {
    KeyMapID keyMapID = new KeyMapID(key, mapID);
    List<KeyMapID> list = this.store.get(id);
    if (list == null) { return; }

    Iterator<KeyMapID> iterator = list.iterator();
    while (iterator.hasNext()) {
      KeyMapID tempKeyMapID = iterator.next();
      if (tempKeyMapID.equals(keyMapID)) {
        iterator.remove();
        break;
      }
    }

    if (list.size() == 0) {
      this.store.remove(id);
    }
  }

  public void flush(final L id) {
    final List<KeyMapID> list;
    ReentrantReadWriteLock lock = getLock(id);
    try {
      lock.writeLock().lock();
      list = this.store.remove(id);
    } finally {
      lock.writeLock().unlock();
    }
    dispose(list);
  }

  private void dispose(List<KeyMapID> list) {
    if (list == null) { return; }
    for (KeyMapID keyMapID : list) {
      remoteServerMapManager.dispose(keyMapID.getMapID(), keyMapID.getKey());
    }
  }

  /**
   * Used only in handshake
   */
  public Set addAllKeysTo(Set keySet) {
    for (ReentrantReadWriteLock readWriteLock : readWriteLocks) {
      readWriteLock.readLock().lock();
    }
    try {
      keySet.addAll(this.store.keySet());
      return keySet;
    } finally {
      for (ReentrantReadWriteLock readWriteLock : readWriteLocks) {
        readWriteLock.readLock().unlock();
      }
    }
  }

  private ReentrantReadWriteLock getLock(L key) {
    return readWriteLocks[key.hashCode()];
  }

  public static class KeyMapID {
    private final Object   key;
    private final ObjectID mapID;

    public KeyMapID(Object key, ObjectID mapID) {
      this.key = key;
      this.mapID = mapID;
    }

    public Object getKey() {
      return key;
    }

    public ObjectID getMapID() {
      return mapID;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + ((mapID == null) ? 0 : (int) mapID.toLong());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      KeyMapID other = (KeyMapID) obj;
      if (key == null) {
        if (other.key != null) return false;
      } else if (!key.equals(other.key)) return false;
      if (mapID == null) {
        if (other.mapID != null) return false;
      } else if (mapID.toLong() != other.mapID.toLong()) return false;
      return true;
    }

  }
}
