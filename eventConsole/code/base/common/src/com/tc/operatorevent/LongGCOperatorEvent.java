/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;


public class LongGCOperatorEvent implements TerracottaOperatorEvent{
  
  private final String time;
  private final String eventMessage;
  private final int eventType;
  private final String nodeId;
  
  public LongGCOperatorEvent(int eventType, String time, String nodeId, String message) {
    this.eventType = eventType;
    this.time = time;
    this.nodeId = nodeId;
    this.eventMessage = message;
  }
  
  public String getEventTime() {
    return this.time;
  }

  public String getEventSystem() {
    return LONG_GC;
  }

  public int getEventType() {
    return this.eventType;
  }

  public String getEventMessage() {
    return this.eventMessage;
  }

  public String getNodeId() {
    return this.nodeId;
  }

}
