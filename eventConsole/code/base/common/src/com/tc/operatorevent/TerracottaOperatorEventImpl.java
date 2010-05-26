/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.util.Assert;

import java.util.Date;

public class TerracottaOperatorEventImpl implements TerracottaOperatorEvent {
  private final long           time;
  private final String         eventMessage;
  private final EventType      eventType;
  private final EventSubsystem subSystem;
  private String               nodeId = null;

  public TerracottaOperatorEventImpl(EventType eventType, EventSubsystem subSystem, String message) {
    this.eventType = eventType;
    this.subSystem = subSystem;
    this.time = System.currentTimeMillis();
    this.eventMessage = message;
  }

  public String getEventMessage() {
    return this.eventMessage;
  }

  public Date getEventTime() {
    return new Date(this.time);
  }

  public EventType getEventType() {
    return this.eventType;
  }

  public String getEventTypeAsString() {
    return this.eventType.name();
  }

  public String getNodeName() {
    return this.nodeId;
  }

  public void setNodeName(String nodeId) {
    Assert.assertNull(this.nodeId);
    this.nodeId = nodeId;
  }

  public EventSubsystem getEventSubsystem() {
    return this.subSystem;
  }

  public String getEventSubsystemAsString() {
    return this.subSystem.name();
  }

}
