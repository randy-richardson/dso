/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.criticalevents;

public class LongGCEvent implements CriticalEvent{

  public String getEventName() {
    return LONG_GC;
  }

  public int getEventType() {
    return CRITICAL;
  }

}
