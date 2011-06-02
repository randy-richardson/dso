/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.locks.LockID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Should be simply implemented by CachedItemStore present in RemoteServerMapManagerImpl
 */
public class L1ServerMapLocalCacheStoreListenerImpl implements
    L1ServerMapLocalCacheStoreListener<Object, LocalCacheStoreValue> {
  private final ServerMapLocalCache serverMapLocalCache;

  public L1ServerMapLocalCacheStoreListenerImpl(ServerMapLocalCache serverMapLocalCache) {
    this.serverMapLocalCache = serverMapLocalCache;
  }

  public void notifyElementEvicted(Object key, LocalCacheStoreValue value) {
    notifyElementsEvicted(Collections.singletonMap(key, value));
  }

  // TODO: does this need to be present in the interface? not called from outside
  public void notifyElementsEvicted(Map<Object, LocalCacheStoreValue> evictedElements) {
    final Set<LockID> evictedLockIds = new HashSet<LockID>();

    for (Entry<Object, LocalCacheStoreValue> entry : evictedElements.entrySet()) {
      // check if incoherent
      LocalCacheStoreValue value = entry.getValue();

      // if eventual
      if (value.isUnlockedCoherent()) {
        this.serverMapLocalCache.flush(value.getID());
      } else if (value.isIncoherent()) {
        // incoherent
        // do nothing
      } else {
        // strong
        evictedLockIds.add((LockID) value.getID());
      }
    }

    if (evictedLockIds.size() > 0) {
      this.serverMapLocalCache.clearForIDsAndRecallLocks(evictedLockIds);
    }
  }
}
