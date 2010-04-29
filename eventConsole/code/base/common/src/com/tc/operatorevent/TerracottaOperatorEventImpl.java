/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.Date;

public class TerracottaOperatorEventImpl implements TerracottaOperatorEvent {
  private final Date           time;
  private final String         eventMessage;
  private final EventType      eventType;
  private final EventSubSystem subSystem;
  protected String             nodeId = null;

  public TerracottaOperatorEventImpl(EventType eventType, EventSubSystem subSystem, Date time, String message) {
    this.eventType = eventType;
    this.subSystem = subSystem;
    this.time = time;
    this.eventMessage = message;
  }

  public String getEventMessage() {
    return this.eventMessage;
  }

  public Date getEventTime() {
    return this.time;
  }

  public EventType getEventType() {
    return this.eventType;
  }

  public String getNodeId() {
    return this.nodeId;
  }

  public synchronized void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public EventSubSystem getEventSubSystem() {
    return this.subSystem;
  }

}
