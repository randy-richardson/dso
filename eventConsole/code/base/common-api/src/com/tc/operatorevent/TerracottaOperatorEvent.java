/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.io.Serializable;
import java.util.Date;


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
  
  String getEventTypeString();
  
  Date getEventTime();
  
  EventSubsystem getEventSubsystem();
  
  String getEventMessage();
  
  String getEventSubsystemString();
  
}
