/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class GarbageCollectContext implements EventContext {

  private final GCType type;
  private long         delay;

  public GarbageCollectContext(final GCType type, final long delay) {
    this.type = type;
    this.delay = delay;
  }

  public GarbageCollectContext(final GCType type) {
    this(type, 0);
  }

  public GCType getType() {
    return type;
  }

  public long getDelay() {
    return delay;
  }

  public void setDelay(final long delay) {
    this.delay = delay;
  }
}
