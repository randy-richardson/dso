/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
    // TODO: should the flushing logic be done inside another thread, since this might delay "put" if eviction called
    // within that thread
    final Set<LockID> evictedLockIds = new HashSet<LockID>();
    Set<Entry> entries = evictedElements.entrySet();

    for (Entry entry : entries) {
      if (!(entry.getValue() instanceof AbstractLocalCacheStoreValue)) {
        continue;
      }

      // check if incoherent
      AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) entry.getValue();

      // if eventual
      if (value.isEventualConsistentValue()) {
        this.serverMapLocalCache.removeEntriesForObjectId(value.asEventualValue().getObjectId());
      } else if (value.isIncoherentValue()) {
        // incoherent
        // do nothing
      } else if (value.isStrongConsistentValue()) {
        // strong
        evictedLockIds.add(value.asStrongValue().getLockId());
      } else {
        throw new AssertionError("AbstractLocalCacheStoreValue should be one of: STRONG, EVENTUAL, INCOHERENT");
      }
    }

    if (evictedLockIds.size() > 0) {
      this.serverMapLocalCache.clearForIDsAndRecallLocks(evictedLockIds);
    }
  }
}
