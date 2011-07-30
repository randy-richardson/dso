/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class PeriodicGarbageCollectContext extends GarbageCollectContext {
  private final long interval;

  public PeriodicGarbageCollectContext(final GCType type, final long interval) {
    super(type, interval);
    this.interval = interval;
  }

  public long getInterval() {
    return interval;
  }

  public void reset() {
    setDelay(interval);
  }
}
