/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

public class LongGCOperatorEvent extends AbstractTerracottaOperatorEvent implements TerracottaOperatorEvent {

  public LongGCOperatorEvent(String eventType, String time, String message) {
    super(eventType, time, message);
  }

  public String getEventSystem() {
    return TerracottaOperatorEvent.SUB_SYSTEM.MEMORY_MANAGER.name();
  }

}
