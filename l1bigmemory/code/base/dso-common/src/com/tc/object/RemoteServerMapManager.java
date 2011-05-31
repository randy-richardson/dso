/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.invalidation.Invalidations;
import com.tc.local.cache.store.DisposeListener;
import com.tc.local.cache.store.LocalCacheStoreValue;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.cache.CachedItem;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.locks.LockID;
import com.tc.object.session.SessionID;

import java.util.Collection;
import java.util.Set;

public interface RemoteServerMapManager extends ClientHandshakeCallback {
  public void addDisposeListener(ObjectID mapID, DisposeListener listener);

  public Object getMappingForKey(ObjectID mapID, Object portableKey);

  public Set getAllKeys(ObjectID mapID);

  public long getAllSize(ObjectID[] mapIDs);

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID,
                                            Collection<ServerMapGetValueResponse> responses, NodeID nodeID);

  public void addResponseForGetAllKeys(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                       Set keys, NodeID nodeID);

  public void addResponseForGetAllSize(SessionID localSessionID, GroupID groupID, ServerMapRequestID requestID,
                                       Long size, NodeID sourceNodeID);

  public void objectNotFoundFor(SessionID sessionID, ObjectID mapID, ServerMapRequestID requestID, NodeID nodeID);

  /**
   * Adds this CachedItem to LockID or ObjectID. When the lock is recalled or the Object is invalidated this CachedItem
   * will be invalidated too.
   */
  public void addCachedItem(Object id, ObjectID mapID, Object key, LocalCacheStoreValue item);

  /**
   * Removes the mapping from ObjectID or LockID to CachedItem
   */
  public void removeCachedItem(Object id, ObjectID mapID, Object key, LocalCacheStoreValue item);

  public void flush(Object id);

  public void flush(Invalidations invalidations);

  public void clearCachedItemsForLocks(Set<LockID> toEvict);

  public void expired(TCObjectServerMap serverMap, CachedItem ci);

  public void dispose(ObjectID mapID, Object key);
}
