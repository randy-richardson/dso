/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.concurrent.TimeUnit;

public class LocalCacheStoreValue {
  private static final long SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS = TCPropertiesImpl
                                                                                      .getProperties()
                                                                                      .getLong(
                                                                                               TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_READ_TIMEOUT);
  /**
   * This corresponds to a ObjectID/LockID
   */
  private final Object      id;
  /**
   * this is the value object <br>
   * TODO: make this Serializable. This would be a SerializedEntry for the serialized caches.
   */
  private final Object      value;

  /**
   * TODO: will need to refactor this in a better way
   */
  private final boolean     isIncoherent;
  private final long        lastCoherentTime;

  public LocalCacheStoreValue(Object id, Object value) {
    this(id, value, false);
  }

  public LocalCacheStoreValue(Object id, Object value, boolean isIncoherent) {
    this.id = id;
    this.value = value;
    this.isIncoherent = isIncoherent;
    this.lastCoherentTime = System.nanoTime();
  }

  public Object getID() {
    return id;
  }

  public Object getValue() {
    return value;
  }

  public boolean isIncoherent() {
    return isIncoherent;
  }

  public boolean isUnlockedCoherent() {
    return id instanceof ObjectID;
  }

  public boolean isIncoherentTooLong() {
    return TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - this.lastCoherentTime)) >= SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS;
  }
}
