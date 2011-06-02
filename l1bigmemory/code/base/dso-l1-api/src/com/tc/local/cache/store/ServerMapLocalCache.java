/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.local.cache.store;

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
   * Pin the entry for this object key. That is this entry should not be evicted.
   */
  void pinEntry(Object key);

  /**
   * Unpin entry for this object key. That is evict it if required.
   */
  void unpinEntry(Object key);

  /**
   * Remove this element from the local cache. Note that this doesn't initiate recalls.
   */
  void evictFromLocalCache(Object key, LocalCacheStoreValue value);

  /**
   * Used in handshake to send a list of ObjectIDs to the server
   */
  void addAllObjectIDsToValidate(Map map);

  /**
   * TCObjectServerMap methods
   */

  /**
   * Add a coherent value to the cache
   */
  void addCoherentValueToCache(Object id, Object key, Object value, boolean isMutate);

  /**
   * Add a coherent value to the cache. This method is called when a remove operation is called from CDSM.
   */
  void addCoherentValueToCache(Object id, Object key, Object value, boolean isMutate, boolean isRemove);

  /**
   * Add a incoherent value to the cache
   */
  void addIncoherentValueToCache(Object key, Object value, boolean isMutate);

  /**
   * Get a coherent value from the local cache. If an incoherent value is present, then return null.
   */
  LocalCacheStoreValue getCoherentLocalValue(Object key);

  /**
   * Get the value corresponding to the key if present
   */
  LocalCacheStoreValue getLocalValue(Object key);

  /**
   * Returns the size of the local cache ...<br>
   * TODO: Do we need to support this
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
   * Called when l1 cache manager tries to free memory
   */
  int evictCachedEntries(int toClear);

  /**
   * Used in non stop cache
   */
  Set getKeySet();
}
