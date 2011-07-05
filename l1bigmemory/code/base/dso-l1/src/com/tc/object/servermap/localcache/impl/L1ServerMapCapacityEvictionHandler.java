/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

public class L1ServerMapCapacityEvictionHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(EventContext context) {
    L1ServerMapCapacityEvictionContext capacityEvictionContext = (L1ServerMapCapacityEvictionContext) context;
    evictFromCache(capacityEvictionContext.getServerMapLocalCache(), capacityEvictionContext.getMaxElementsInMemory());
  }

  private void evictFromCache(ServerMapLocalCache serverMapLocalCache, int maxElementsInMemory) {
    try {
      int overshoot = serverMapLocalCache.size() - maxElementsInMemory;
      if (overshoot <= 0) { return; }

      int elementsToEvict = (int) ((maxElementsInMemory * 20.0) / 100 + overshoot);
      serverMapLocalCache.evictCachedEntries(elementsToEvict);
    } finally {
      serverMapLocalCache.evictionCompleted();
    }
  }
}
