/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

import java.util.Map;
import java.util.Set;

public interface ServerMapLocalCache {
  ObjectID getMapID();

  void flush(Object id);

  void clearForIDsAndRecallLocks(Set<LockID> evictedLockIds);

  void pinEntry(Object key);

  void unpinEntry(Object key);

  void evictFromLocalCache(Object key, LocalCacheStoreValue value);

  void addAllObjectIDsToValidate(Map map);
}
