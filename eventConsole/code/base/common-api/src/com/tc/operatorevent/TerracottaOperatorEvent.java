/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.io.Serializable;


public interface TerracottaOperatorEvent extends Serializable {
  
  public static enum EventType {
    INFO, WARN, DEBUG, ERROR, CRITICAL
  }

  public static enum EventSubsystem {
    MEMORY_MANAGER, DGC, HA
  }
  
  void setNodeId(String nodeId);

  String getNodeId();

  EventType getEventType();
  
  long getEventTime();
  
  EventSubsystem getEventSubsystem();
  
  String getEventMessage();
  
}
