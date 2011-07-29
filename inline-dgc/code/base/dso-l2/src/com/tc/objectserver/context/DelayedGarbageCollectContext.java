/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class DelayedGarbageCollectContext extends GarbageCollectContext {

  private final long delay;

  public DelayedGarbageCollectContext(final GCType type, final boolean reschedule, final long delay) {
    super(type, reschedule);
    this.delay = delay;
  }

  public long getDelay() {
    return delay;
  }
}
