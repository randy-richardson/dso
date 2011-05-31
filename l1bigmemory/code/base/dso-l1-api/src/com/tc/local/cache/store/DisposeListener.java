/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

public interface DisposeListener {
  /**
   * This method is called when the CachedItem is disposed. Dispose is called when the item is being flushed/removed
   * from the RemoteServerMapManager
   */
  public void disposed(Object key);

  public void pinEntry(Object key);

  public void unpinEntry(Object key);

  /**
   * This method is called to remove from the LocalCache if the same mapping exists from the key to the cachedItem. This
   * will also remove from the RemoteServerMapManager
   */
  public void evictFromLocalCache(Object key, LocalCacheStoreValue ci);
}
