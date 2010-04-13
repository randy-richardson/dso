/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.subsystemevent.stats;

import com.tc.net.NodeID;
import com.tc.stats.TerracottaSubSystemEventStats;

import java.util.Date;

public class TerracottaSubSystemEventsStatsImpl implements TerracottaSubSystemEventStats {

  private NodeID nodeId;
  private String eventType;
  private Date   startTime;
  
  public TerracottaSubSystemEventsStatsImpl(String eventType) {
    this.nodeId = null;
    this.eventType = eventType;
    this.startTime = new Date();
  }

  public synchronized void setNodeId(NodeID nodeId) {
    this.nodeId = nodeId;
  }

  public synchronized void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public synchronized void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEventTime() {
    return this.startTime;
  }

  public NodeID getNodeID() {
    return this.nodeId;
  }

  public String getTCEventType() {
    return this.eventType;
  }

  public String getMessage() {
    return "Long GC Happened";
  }

  public String getSystem() {
    return "MEMORY MONITOR";
  }

}
