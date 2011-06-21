/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import java.util.Set;

/**
 * The backing Cache Store for the Local Cache present in TCObjectServerMapImpl
 */
public interface L1ServerMapLocalCacheStore<K, V> {

  /**
   * Put an entry in the backing map<br>
   * 
   * @return the old value if present
   */
  public V put(K key, V value);

  /**
   * Put an entry in the backing map<br>
   * 
   * @param pinned true if the entry is not to be considered for eviction
   * @return the old value if present
   */
  public V put(K key, V value, boolean pinned);

  /**
   * @return the value if present
   */
  public V get(K key);

  /**
   * Remove an entry in the backing map<br>
   * 
   * @return the old value if present
   */
  public V remove(K key);

  /**
   * Add a listener which will get called when <br>
   * 1) capacity eviction evicts entries from map<br>
   * 2) evict (count) method evicts entries from map<br>
   */
  public boolean addListener(L1ServerMapLocalCacheStoreListener<K, V> listener);

  /**
   * Removes the added listener
   */
  public boolean removeListener(L1ServerMapLocalCacheStoreListener<K, V> listener);

  /**
   * evict "count" number of entries from the backing map
   */
  public int evict(int count);

  /**
   * Clear the map
   */
  public void clear();

  /**
   * @return key set for this map
   */
  public Set getKeySet();

  /**
   * @return size of the map
   */
  public int size();

  /**
   * Unpin entry so that it is eligible for eviction
   */
  public void unpinEntry(K key);
}
