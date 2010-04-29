/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;


public abstract class AbstractTerracottaOperatorEvent implements TerracottaOperatorEvent {

  protected final String time;
  protected final String eventMessage;
  protected final String eventType;
  protected String       nodeId = null;

  public AbstractTerracottaOperatorEvent(String eventType, String time, String message) {
    this.eventType = eventType;
    this.time = time;
    this.eventMessage = message;
  }


  public String getEventMessage() {
    return this.eventMessage;
  }

  public String getEventTime() {
    return this.time;
  }

  public String getEventType() {
    return this.eventType;
  }

  public String getNodeId() {
    return this.nodeId;
  }

  public synchronized void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

}
