/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

public class L1ServerMapCapacityEvictionHandler extends AbstractEventHandler {

  @Override
  public void handleEvent(EventContext context) {
    L1ServerMapCapacityEvictionContext capacityEvictionContext = (L1ServerMapCapacityEvictionContext) context;
    evictFromCache(capacityEvictionContext.getServerMapLocalCacheStore(), capacityEvictionContext
        .getMaxElementsInMemory(), capacityEvictionContext.getL1ServerMapCapacityEvictionContext());
  }

  private void evictFromCache(L1ServerMapLocalCacheStore serverMapLocalCacheStore, int maxElementsInMemory,
                              L1ServerMapLocalStoreEvictionInfo l1ServerMapLocalStoreEvictionInfo) {
    try {
      int overshoot = serverMapLocalCacheStore.size() - maxElementsInMemory;
      if (overshoot <= 0) { return; }

      int elementsToEvict = (int) ((maxElementsInMemory * 20.0) / 100 + overshoot);
      serverMapLocalCacheStore.evict(elementsToEvict);
    } finally {
      l1ServerMapLocalStoreEvictionInfo.notifyEvictionCompleted();
    }
  }
}
