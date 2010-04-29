/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.io.Serializable;


public interface TerracottaOperatorEvent extends Serializable {
  
  public static enum EVENT_TYPE {
    INFO, WARN, DEBUG, ERROR, CRITICAL
  }

  public static enum SUB_SYSTEM {
    MEMORY_MANAGER, DGC, HA
  }
  
  String getEventTime();
  
  String getNodeId();
  
  void setNodeId(String nodeId);

  String getEventType();

  String getEventSystem();
  
  String getEventMessage();
  
}
