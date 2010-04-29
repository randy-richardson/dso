/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

public class ServerNodeJoinedEvent extends AbstractTerracottaOperatorEvent implements TerracottaOperatorEvent {

  public ServerNodeJoinedEvent(String eventType, String time, String message) {
    super(eventType, time, message);
  }

  public String getEventSystem() {
    return TerracottaOperatorEvent.SUB_SYSTEM.HA.name();
  }

}
