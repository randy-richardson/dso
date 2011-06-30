/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.LockID;

import java.util.Map;
import java.util.Set;

/**
 * A Global cache manager which contains info about all the LocalCache present in the L1.<br>
 * This acts a multiplexer between RemoteServerMapManager, HandshakeManager and the LocalCaches present
 */
public interface GlobalLocalCacheManager {

  /**
   * Initialize the global local cache manager
   */
  public void initialize(RemoteServerMapManager remoteServerMapManager);

  /**
   * Create a local cache for use or return already created local cache for the mapId
   */
  public ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager, Manager manager,
                                                   boolean localCacheEnabled);

  /**
   * Recall locks. Called from LocalCache.
   */
  public void recallLocks(Set<LockID> toEvict);

  /**
   * flush the entries from the LocalCache associated with the given map id.<br>
   * This is used in the process of invalidations
   */
  public void flush(ObjectID mapID, Set<ObjectID> set);

  /**
   * Used when a lock recall happens<br>
   * All the local cache entries associated with this lock id will be removed
   */
  public void flush(LockID lockID);

  /**
   * Handshake manager tries to get hold of all the objects present in the local caches
   */
  public Map addAllObjectIDsToValidate(Map map);

  /**
   * Remember the mapId associated with the valueLockId
   */
  public void rememberMapIdForValueLockId(LockID valueLockId, ObjectID mapID);

}
