/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

import java.util.Map;
import java.util.Set;

public interface GlobalLocalCacheManager {
  public void addLocalCache(ObjectID mapID, ServerMapLocalCache listener);

  public void removeLocalCache(ObjectID mapID);

  public void recallLocks(Set<LockID> toEvict);

  public void flush(ObjectID mapID, Set<ObjectID> set);

  public void flush(LockID lockID);

  public Map addAllObjectIDsToValidate(Map map);
}
