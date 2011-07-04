/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;

import java.util.Map;

public interface ServerMapLocalCacheIdStore {

  /**
   * Set up the local store for use
   */
  void setupLocalStore(L1ServerMapLocalCacheStore localStore);

  /**
   * Add the key to the list of keys mapped by the id
   */
  void addIdKeyMapping(Object id, Object key);

  /**
   * Removes the key from the list of keys mapped by the id
   */
  void removeIdKeyMapping(Object id, Object key);

  /**
   * Remove all entries in the local store corresponding to the keys mapped by id
   */
  void removeEntries(Object id);

  /**
   * Remove the entry in the local store corresponding to the particular key if mapped by id. Also removes the key from
   * the list of keys mapped by id
   */
  void evictedFromStore(Object id, Object key);

  /**
   * Clears all id->list(keys) mapping, also clears the internal store
   */
  void clearAllEntries();

  /**
   * Add all objects to validate in the map using the mapId
   */
  void addAllObjectIDsToValidate(ObjectID mapId, Map map);

  /**
   * Remove entry for particular key
   */
  void removeEntryForKey(Object key);
}
