/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.Sink;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;

import java.util.concurrent.atomic.AtomicBoolean;

public class L1ServerMapLocalStoreEvictionInfo {
  private final AtomicBoolean isEvictionInProgress = new AtomicBoolean(false);
  private final int           maxElementsInMemory;

  public L1ServerMapLocalStoreEvictionInfo(final int maxElementsInMemory) {
    this.maxElementsInMemory = maxElementsInMemory;
  }

  public void initiateCapacityEvictionIfRequired(L1ServerMapLocalCacheStore store, Sink capacityEvictionSink) {
    if (isEvictionInProgress.get() || store.size() < maxElementsInMemory) { return; }

    isEvictionInProgress.set(true);
    L1ServerMapCapacityEvictionContext capacityEvictionContext = new L1ServerMapCapacityEvictionContext(store, this,
                                                                                                        maxElementsInMemory);
    capacityEvictionSink.add(capacityEvictionContext);
  }

  public void notifyEvictionCompleted() {
    isEvictionInProgress.set(false);
  }
}