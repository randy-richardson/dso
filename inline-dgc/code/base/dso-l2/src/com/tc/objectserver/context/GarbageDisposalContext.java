/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

import java.util.SortedSet;

public class GarbageDisposalContext implements EventContext {
  private final SortedSet<ObjectID> garbage;

  public GarbageDisposalContext(SortedSet<ObjectID> garbage) {
    this.garbage = garbage;
  }

  public SortedSet<ObjectID> getGarbageIDs() {
    return garbage;
  }
}
