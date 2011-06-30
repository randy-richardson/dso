/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.object.locks.LockID;

import java.util.Map;
import java.util.Set;

public interface ServerMapLocalCache {

  /**
   * Set up local cache store for use
   */
  void setupLocalStore(L1ServerMapLocalCacheStore serverMapLocalStore);

  /**
   * Each ServerMapLocalCache cache is associated with a TCObjectServerMap<br>
   * This ObjectID is the id of that TCObject
   */
  ObjectID getMapID();

  /**
   * Remove the entry associated with this id from the LocalCache and the ID store as well
   */
  void flush(Object id);

  /**
   * Same as flush and also initiate recall
   */
  void clearForIDsAndRecallLocks(Set<LockID> evictedLockIds);

  /**
   * Unpin entry for this object key. That is evict it if required.
   */
  void unpinEntry(Object key);

  /**
   * Remove this element from the local cache. Note that this doesn't initiate recalls.
   */
  void evictFromLocalCache(Object key, AbstractLocalCacheStoreValue value);

  /**
   * Used in handshake to send a list of ObjectIDs to the server
   */
  void addAllObjectIDsToValidate(Map map);

  // ///////////////////////////////
  // TCObjectServerMap methods
  // ///////////////////////////////

  /**
   * Cache strong consistent values
   */
  void addStrongValueToCache(LockID lockId, Object key, Object value, MapOperationType operationType);

  /**
   * Cache eventual consistent values
   */
  void addEventualValueToCache(ObjectID valueObjectId, Object key, Object value, MapOperationType operationType);

  /**
   * Cache incoherent/bulk-load values
   */
  void addIncoherentValueToCache(Object key, Object value, MapOperationType operationType);

  /**
   * Get a coherent value from the local cache. If an incoherent value is present, then return null.
   */
  AbstractLocalCacheStoreValue getCoherentLocalValue(Object key);

  /**
   * Get the value corresponding to the key if present
   */
  AbstractLocalCacheStoreValue getLocalValue(Object key);

  /**
   * Returns the size of the local cache ...<br>
   */
  int size();

  /**
   * clear all elements from the local cache
   */
  void clearAllLocalCache();

  /**
   * Remove a key from the Local cache, this will try to recall locks if possible
   */
  void removeFromLocalCache(Object key);

  /**
   * Attempt to remove 'count' entries from the local cache. May be called under memory pressure
   */
  int evictCachedEntries(int count);

  /**
   * Returns the keys present in the local cache (does not include meta items stored)
   */
  Set getKeySet();
}
