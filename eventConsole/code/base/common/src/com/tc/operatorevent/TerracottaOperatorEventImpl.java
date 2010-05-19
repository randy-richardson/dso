/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.net.NodeID;
import com.tc.util.Assert;

import java.util.Date;

public class TerracottaOperatorEventImpl implements TerracottaOperatorEvent {
  private final long           time;
  private final String         eventMessage;
  private final EventType      eventType;
  private final EventSubsystem subSystem;
  private NodeID               nodeId = null;

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

  public NodeID getNodeId() {
    return this.nodeId;
  }
  
  public String getNodeIdAsString() {
    return this.nodeId.toString();
  }

  public void setNodeId(NodeID nodeId) {
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
