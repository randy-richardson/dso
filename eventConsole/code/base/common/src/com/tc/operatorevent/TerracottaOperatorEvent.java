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

  public static enum EventSubSystem {
    MEMORY_MANAGER, DGC, HA
  }
  
  Date getEventTime();
  
  String getNodeId();
  
  void setNodeId(String nodeId);

  EventType getEventType();

  EventSubSystem getEventSubSystem();
  
  String getEventMessage();
  
}
