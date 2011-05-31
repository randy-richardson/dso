/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import java.util.Map;

public interface L1ServerMapLocalCacheStoreListener<K, V> {
  public void notifyElementEvicted(K key, V value);

  public void notifyElementsEvicted(Map<K, V> evictedElements);
}
