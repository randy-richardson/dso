/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;


public class DGCTerracottaOperatorEvent extends AbstractTerracottaOperatorEvent implements TerracottaOperatorEvent {
  
  public DGCTerracottaOperatorEvent(String eventType, String time, String message) {
    super(eventType, time, message);
  }

  public String getEventSystem() {
    return TerracottaOperatorEvent.SUB_SYSTEM.DGC.name();
  }

}
