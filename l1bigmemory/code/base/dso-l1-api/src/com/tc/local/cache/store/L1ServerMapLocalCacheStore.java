/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import java.util.Set;

public interface L1ServerMapLocalCacheStore<K, V> {
  public V put(K key, V value);

  public V get(K key);

  public void pinEntry(K key);

  public void unpinEntry(K key);

  /**
   * remove and put wont call the Eviction Listener
   */
  public V remove(K key);

  /**
   * evict and clearAllLocalCache will call the Eviction Listener
   */
  public int evict(int count);

  public void clear();

  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener);

  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener);

  public Set getKeySet();

  public int size();
}
