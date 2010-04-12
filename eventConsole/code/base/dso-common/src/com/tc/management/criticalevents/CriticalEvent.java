/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.criticalevents;

public interface CriticalEvent {
  public static final int CRITICAL = 0;
  
  public static final String LONG_GC = "LONG_GC";
  
  String getEventName();
  
  int getEventType();
  
}
