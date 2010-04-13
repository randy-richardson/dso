/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.tcevent;

public class LongGCEvent implements TerracottaSubSystemEvent{

  public String getEventName() {
    return LONG_GC;
  }

  public int getEventType() {
    return CRITICAL;
  }

}
