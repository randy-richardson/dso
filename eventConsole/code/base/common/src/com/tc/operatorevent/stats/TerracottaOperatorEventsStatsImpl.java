/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent.stats;

import com.tc.stats.TerracottaOperatorEventStats;

public class TerracottaOperatorEventsStatsImpl implements TerracottaOperatorEventStats {

  private final String eventType;
  private final String time;
  private final String eventMessage;
  private final String nodeId;
  private final String eventSystem;

  public TerracottaOperatorEventsStatsImpl(String eventTime, String eventType, String eventSystem, String nodeId,
                                           String eventMessage) {
    this.nodeId = nodeId;
    this.eventType = eventType;
    this.time = eventTime;
    this.eventMessage = eventMessage;
    this.eventSystem = eventSystem;
  }

  public String getEventTime() {
    return this.time;
  }

  public String getNodeId() {
    return this.nodeId;
  }

  public String getEventType() {
    return this.eventType;
  }

  public String getEventMessage() {
    return this.eventMessage;
  }

  public String getEventSubsystem() {
    return this.eventSystem;
  }

}
