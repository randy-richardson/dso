/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.object.servermap.localcache;

import java.util.List;

/**
 * The backing Cache Store for the Local Cache present in TCObjectServerMapImpl
 */
public interface L1ServerMapLocalCacheStore<K, V> {

  /**
   * Put an entry in the backing map
   * <br>
   * The behavior depends on the putType - the entry may be pinned on put, size may not increment even on put etc
   * 
   * @return the old value if present
   */
  public V put(K key, V value) throws LocalCacheStoreFullException;

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
   * Removes an entry in the backing map if the key is actually mapped to the given value<br>
   * 
   * @return the old value if present
   */
  public Object remove(K key, V value);

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
   * Clear the map
   */
  public void clear();

  public void cleanLocalState();

  /**
   * @return key set for this map
   */
  public List getKeys();

  /**
   * @return size of the map
   */
  public int size();

  /**
   * Bytes this local cache is taking up on heap.
   */
  public long onHeapSizeInBytes();

  /**
   * Bytes this local cache is taking up off heap.
   */
  public long offHeapSizeInBytes();

  /**
   * Get the number of items on the local heap
   */
  public int onHeapSize();

  /**
   * Get the number of items on the local offheap
   */
  public int offHeapSize();

  /**
   * Max elements in memory
   */
  public int getMaxElementsInMemory();

  /**
   * Dispose of this local cache store
   */
  public void dispose();

  /**
   * Check if the key is available on heap
   */
  public boolean containsKeyOnHeap(K key);

  /**
   * Check if the key is available off heap
   */
  public boolean containsKeyOffHeap(K key);

  /**
   * Set the max number entries to be stored in local heap
   */
  public void setMaxEntriesLocalHeap(int maxEntriesLocalHeap);

  /**
   * Set the max local heap usage in bytes
   */
  public void setMaxBytesLocalHeap(long maxBytesLocalHeap);

  public void recalculateSize(K key);

  public boolean isPinned();

}
