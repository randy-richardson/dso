/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

public class TerracottaOperatorEventFactory {

  public static TerracottaOperatorEvent createLongGCOperatorEvent(EventType eventType, String message) {
    return new TerracottaOperatorEventImpl(eventType, EventSubsystem.MEMORY_MANAGER, message);
  }

  public static TerracottaOperatorEvent createDGCOperatorEvent(EventType eventType, String message) {
    return new TerracottaOperatorEventImpl(eventType, EventSubsystem.DGC, message);
  }

  public static TerracottaOperatorEvent createServerNodeJoinedOperatorEvent(EventType eventType, String message) {
    return new TerracottaOperatorEventImpl(eventType, EventSubsystem.HA, message);
  }
}
