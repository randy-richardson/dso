/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.operatorevent.TerracottaOperatorEvent.EventSubSystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

import java.util.Date;

public class TerracottaOperatorEventFactory {

  public static TerracottaOperatorEvent createLongGCOperatorEvent(EventType eventType, Date time, String message) {
    return new TerracottaOperatorEventImpl(eventType, EventSubSystem.MEMORY_MANAGER, time, message);
  }

  public static TerracottaOperatorEvent createDGCOperatorEvent(EventType eventType, Date time, String message) {
    return new TerracottaOperatorEventImpl(eventType, EventSubSystem.DGC, time, message);
  }
  
  public static TerracottaOperatorEvent createServerNodeJoinedOperatorEvent(EventType eventType, Date time,
                                                                            String message) {
    return new TerracottaOperatorEventImpl(eventType, EventSubSystem.HA, time, message);
  }
}
