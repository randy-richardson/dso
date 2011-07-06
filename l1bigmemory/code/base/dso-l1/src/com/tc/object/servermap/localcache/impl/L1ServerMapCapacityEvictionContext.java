/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

public class L1ServerMapCapacityEvictionContext implements EventContext {
  private final L1ServerMapLocalCacheStore        serverMapLocalCacheStore;
  private final int                               maxElementsInMemory;
  private final L1ServerMapLocalStoreEvictionInfo l1ServerMapLocalStoreEvictionInfo;

  public L1ServerMapCapacityEvictionContext(L1ServerMapLocalCacheStore serverMapLocalCacheStore,
                                            L1ServerMapLocalStoreEvictionInfo l1ServerMapLocalStoreEvictionInfo,
                                            int maxElementsInMemory) {
    this.serverMapLocalCacheStore = serverMapLocalCacheStore;
    this.maxElementsInMemory = maxElementsInMemory;
    this.l1ServerMapLocalStoreEvictionInfo = l1ServerMapLocalStoreEvictionInfo;
  }

  public L1ServerMapLocalCacheStore getServerMapLocalCacheStore() {
    return serverMapLocalCacheStore;
  }

  public int getMaxElementsInMemory() {
    return maxElementsInMemory;
  }

  public L1ServerMapLocalStoreEvictionInfo getL1ServerMapCapacityEvictionContext() {
    return l1ServerMapLocalStoreEvictionInfo;
  }
}
