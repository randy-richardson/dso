/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

public class LongGCOperatorEvent implements TerracottaOperatorEvent {

  private final String time;
  private final String eventMessage;
  private final int    eventType;
  private String       nodeId = null;

  public LongGCOperatorEvent(int eventType, String time, String message) {
    this.eventType = eventType;
    this.time = time;
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

  public synchronized void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

}
