/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryL1ServerMapLocalCacheStore<K, V> implements L1ServerMapLocalCacheStore<K, V> {
  private static final int                                     CONCURRENCY = 16;
  private final Map<K, StoreValue<V>>[]                        stores      = new Map[CONCURRENCY];
  private final ReentrantReadWriteLock[]                       locks       = new ReentrantReadWriteLock[CONCURRENCY];

  private final List<L1ServerMapLocalCacheStoreListener<K, V>> listeners   = new CopyOnWriteArrayList<L1ServerMapLocalCacheStoreListener<K, V>>();
  private final int                                            maxElementInMemory;
  private final AtomicInteger                                  size        = new AtomicInteger();

  public InMemoryL1ServerMapLocalCacheStore(int maxInMemory) {
    for (int i = 0; i < CONCURRENCY; i++) {
      stores[i] = new HashMap<K, StoreValue<V>>();
      locks[i] = new ReentrantReadWriteLock();
    }

    if (maxInMemory == 0) {
      this.maxElementInMemory = Integer.MAX_VALUE;
    } else {
      this.maxElementInMemory = maxInMemory;
    }
  }

  private Map<K, StoreValue<V>> getStoreFor(K key) {
    int hashCode = Math.abs(key.hashCode());
    return stores[hashCode % CONCURRENCY];
  }

  private ReentrantReadWriteLock getLockFor(K key) {
    int hashCode = Math.abs(key.hashCode());
    return locks[hashCode % CONCURRENCY];
  }

  public V get(K key) {
    Map<K, StoreValue<V>> store = getStoreFor(key);
    ReentrantReadWriteLock lock = getLockFor(key);

    lock.readLock().lock();
    try {
      return store.get(key).getValue();
    } finally {
      lock.readLock().unlock();
    }
  }

  public V put(K key, V value, boolean isPinned) {
    if (!isPinned) { return put(key, value); }

    Map<K, StoreValue<V>> store = getStoreFor(key);
    ReentrantReadWriteLock lock = getLockFor(key);

    V tempVal = null;
    lock.writeLock().lock();
    try {
      StoreValue<V> oldValue = store.get(key);
      if (oldValue != null) {
        tempVal = oldValue.getValue();
        oldValue.setValue(value);
        oldValue.setPinned(true);
      } else {
        StoreValue<V> newValue = new StoreValue<V>(true, value);
        store.put(key, newValue);
        size.incrementAndGet();
      }

    } finally {
      lock.writeLock().unlock();
    }
    doCapacityEviction();

    return tempVal;
  }

  public V put(K key, V value) {
    Map<K, StoreValue<V>> store = getStoreFor(key);
    ReentrantReadWriteLock lock = getLockFor(key);

    V tempVal = null;
    lock.writeLock().lock();
    try {
      StoreValue<V> oldValue = store.get(key);
      if (oldValue != null) {
        tempVal = oldValue.getValue();
        oldValue.setValue(value);
      } else {
        StoreValue<V> newValue = new StoreValue<V>(value);
        store.put(key, newValue);
        size.incrementAndGet();
      }

    } finally {
      lock.writeLock().unlock();
    }
    doCapacityEviction();

    return tempVal;
  }

  /**
   * This method needs to be removed
   */
  public void pinEntry(K key) {
    Map<K, StoreValue<V>> store = getStoreFor(key);
    ReentrantReadWriteLock lock = getLockFor(key);

    lock.writeLock().lock();
    try {
      StoreValue<V> value = store.get(key);
      if (value != null) {
        value.setPinned(true);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void unpinEntry(K key) {
    Map<K, StoreValue<V>> store = getStoreFor(key);
    ReentrantReadWriteLock lock = getLockFor(key);

    lock.writeLock().lock();
    try {
      StoreValue<V> value = store.get(key);
      if (value != null) {
        value.setPinned(false);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public V remove(K key) {
    Map<K, StoreValue<V>> store = getStoreFor(key);
    ReentrantReadWriteLock lock = getLockFor(key);

    lock.writeLock().lock();
    try {
      StoreValue<V> value = store.remove(key);
      if (value != null) {
        size.decrementAndGet();
        return value.getValue();
      }
      return null;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void doCapacityEviction() {
    if (maxElementInMemory == Integer.MAX_VALUE && size.get() <= maxElementInMemory) { return; }

    // try eviction
    evict(size.get() - maxElementInMemory);
  }

  public int evict(int count) {
    Map<K, V> tempMap = new HashMap<K, V>();
    int evicted = 0;

    for (int i = 0; i < CONCURRENCY && evicted < count; i++) {
      Map<K, StoreValue<V>> store = stores[i];
      ReentrantReadWriteLock lock = locks[i];

      lock.writeLock().lock();
      try {
        Iterator<Entry<K, StoreValue<V>>> iterator = store.entrySet().iterator();
        while (iterator.hasNext() && evicted < count) {
          Entry<K, StoreValue<V>> entry = iterator.next();
          K key = entry.getKey();
          StoreValue<V> value = entry.getValue();
          if (!value.isPinned()) {
            iterator.remove();
            size.decrementAndGet();
            evicted++;
            tempMap.put(key, value.getValue());
          }
        }
      } finally {
        lock.writeLock().unlock();
      }
    }

    notifyListeners(tempMap);
    return tempMap.size();
  }

  public int size() {
    return this.size.get();
  }

  public void clear() {
    this.evict(Integer.MAX_VALUE);
  }

  public Set getKeySet() {
    HashSet tempSet = new HashSet();
    for (int i = 0; i < CONCURRENCY; i++) {
      Map<K, StoreValue<V>> store = stores[i];
      ReentrantReadWriteLock lock = locks[i];

      lock.readLock().lock();
      try {
        Set keySet = store.keySet();
        for (Object key : keySet) {
          tempSet.add(key);
        }
      } finally {
        lock.readLock().unlock();
      }
    }
    return tempSet;
  }

  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.add(listener);
  }

  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener) {
    return listeners.remove(listener);
  }

  private void notifyListeners(Map<K, V> evictedElements) {
    for (L1ServerMapLocalCacheStoreListener<K, V> listener : listeners) {
      listener.notifyElementsEvicted(evictedElements);
    }
  }

  private static class StoreValue<V> {
    private V       value;
    private boolean isPinned;

    public StoreValue(V value) {
      this(false, value);
    }

    public StoreValue(boolean isPinned, V value) {
      this.isPinned = isPinned;
      this.value = value;
    }

    public boolean isPinned() {
      return this.isPinned;
    }

    public void setPinned(boolean pinned) {
      this.isPinned = pinned;
    }

    public void setValue(V val) {
      this.value = val;
    }

    public V getValue() {
      return value;
    }
  }

  @Override
  public String toString() {
    return "InMemoryL1ServerMapLocalCacheStore [listeners=" + listeners + ", locks=" + Arrays.toString(locks)
           + ", maxElementInMemory=" + maxElementInMemory + ", stores=" + Arrays.toString(stores) + "]";
  }

}
