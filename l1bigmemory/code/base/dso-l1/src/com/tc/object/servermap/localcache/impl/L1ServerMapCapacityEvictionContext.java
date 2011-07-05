/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

public class L1ServerMapCapacityEvictionContext implements EventContext {
  private final ServerMapLocalCache serverMapLocalCache;
  private final int                 maxElementsInMemory;

  public L1ServerMapCapacityEvictionContext(ServerMapLocalCache serverMapLocalCache, int maxElementsInMemory) {
    this.serverMapLocalCache = serverMapLocalCache;
    this.maxElementsInMemory = maxElementsInMemory;
  }

  public ServerMapLocalCache getServerMapLocalCache() {
    return serverMapLocalCache;
  }

  public int getMaxElementsInMemory() {
    return maxElementsInMemory;
  }
}
