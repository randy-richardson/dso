/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.RemoteServerMapManager;
import com.tc.object.locks.LockID;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Should be simply implemented by CachedItemStore present in RemoteServerMapManagerImpl
 */
public class L1ServerMapLocalCacheStoreListenerImpl implements
    L1ServerMapLocalCacheStoreListener<Object, LocalCacheStoreValue> {
  private final RemoteServerMapManager serverMapManager;

  public L1ServerMapLocalCacheStoreListenerImpl(RemoteServerMapManager serverMapManager) {
    this.serverMapManager = serverMapManager;
  }

  public void notifyElementEvicted(Object key, LocalCacheStoreValue value) {
    HashMap<Object, LocalCacheStoreValue> map = new HashMap<Object, LocalCacheStoreValue>();
    map.put(key, value);

    notifyElementsEvicted(map);
  }

  public void notifyElementsEvicted(Map<Object, LocalCacheStoreValue> evictedElements) {
    final Set<LockID> evictedLockIds = new HashSet<LockID>();

    for (Entry<Object, LocalCacheStoreValue> entry : evictedElements.entrySet()) {
      // check if incoherent
      LocalCacheStoreValue value = entry.getValue();

      // if eventual
      if (value.isUnlockedCoherent()) {
        this.serverMapManager.flush(value.getID());
      } else if (value.isIncoherent()) {
        // incoeherent
        // do nothing
      } else {
        // strong
        evictedLockIds.add((LockID) value.getID());
      }
    }

    if (evictedLockIds.size() > 0) {
      this.serverMapManager.clearCachedItemsForLocks(evictedLockIds);
    }
  }
}
