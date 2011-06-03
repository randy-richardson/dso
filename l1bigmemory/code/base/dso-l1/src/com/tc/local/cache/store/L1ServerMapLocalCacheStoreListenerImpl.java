/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.locks.LockID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Should be simply implemented by CachedItemStore present in RemoteServerMapManagerImpl
 */
public class L1ServerMapLocalCacheStoreListenerImpl implements L1ServerMapLocalCacheStoreListener {
  private final ServerMapLocalCache serverMapLocalCache;

  public L1ServerMapLocalCacheStoreListenerImpl(ServerMapLocalCache serverMapLocalCache) {
    this.serverMapLocalCache = serverMapLocalCache;
  }

  public void notifyElementEvicted(Object key, Object value) {
    notifyElementsEvicted(Collections.singletonMap(key, value));
  }

  // TODO: does this need to be present in the interface? not called from outside
  public void notifyElementsEvicted(Map evictedElements) {
    final Set<LockID> evictedLockIds = new HashSet<LockID>();
    Set<Entry> entries = evictedElements.entrySet();

    for (Entry entry : entries) {
      if (!(entry.getValue() instanceof LocalCacheStoreValue)) {
        continue;
      }

      // check if incoherent
      LocalCacheStoreValue value = (LocalCacheStoreValue) entry.getValue();

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
