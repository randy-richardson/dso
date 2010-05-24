/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.net.NodeID;

import java.io.Serializable;
import java.util.Date;


public interface TerracottaOperatorEvent extends Serializable {
  
  public static enum EventType {
    INFO, WARN, DEBUG, ERROR, CRITICAL
  }

  public static enum EventSubsystem {
    MEMORY_MANAGER, DGC, HA, LOCK_MANAGER
  }
  
  void setNodeId(NodeID nodeId);

  NodeID getNodeId();

  EventType getEventType();
  
  Date getEventTime();
  
  EventSubsystem getEventSubsystem();
  
  String getEventMessage();
  
  /**
   * These methods are there because devconsole does not take enum as the return type while updating the panel Should be
   * dealt with in future
   */
  String getEventTypeAsString();

  String getEventSubsystemAsString();
  
  String getNodeIdAsString();
  
}
