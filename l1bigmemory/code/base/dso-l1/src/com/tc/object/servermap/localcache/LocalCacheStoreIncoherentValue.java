/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.object.ObjectID;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.concurrent.TimeUnit;

public class LocalCacheStoreIncoherentValue extends AbstractLocalCacheStoreValue {
  private static final long SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS = TCPropertiesImpl
                                                                                      .getProperties()
                                                                                      .getLong(
                                                                                               TCPropertiesConsts.EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_READ_TIMEOUT);

  private final long        lastCoherentTime;

  public LocalCacheStoreIncoherentValue(Object value, ObjectID mapID) {
    super(null, value, mapID);
    this.lastCoherentTime = System.nanoTime();
  }

  @Override
  public boolean isIncoherentValue() {
    return true;
  }

  @Override
  public boolean isIncoherentTooLong() {
    return TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - this.lastCoherentTime)) >= SERVERMAP_INCOHERENT_CACHED_ITEMS_RECYCLE_TIME_MILLIS;
  }

}
