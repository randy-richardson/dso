/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.objectserver.dgc.api.GarbageCollector.GCType;

public class GarbageCollectContext implements EventContext {

  private final GCType  type;
  private final boolean reschedule;

  public GarbageCollectContext(final GCType type, final boolean oneShot) {
    this.type = type;
    this.reschedule = oneShot;
  }

  public GCType getType() {
    return type;
  }

  public boolean reschedule() {
    return reschedule;
  }
}
