/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.management.beans.TerracottaOperatorEventsMbean;

public class TerracottaOperatorEventCallbackLogger implements TerracottaOperatorEventCallback {

  private final TCLogger                      tcLogger;
  private final TerracottaOperatorEventsMbean tcOperatorEventsMbean;

  public TerracottaOperatorEventCallbackLogger(TCLogger tcLogger, TerracottaOperatorEventsMbean tcOperatorEventsMbean) {
    this.tcLogger = tcLogger;
    this.tcOperatorEventsMbean = tcOperatorEventsMbean;
  }

  public void fireOperatorEvent(TerracottaOperatorEvent event) {
    logEvent(event);
    this.tcOperatorEventsMbean.fireOperatorEvent(event);
  }

  private void logEvent(TerracottaOperatorEvent event) {
    String eventType = event.getEventType();
    if (eventType.equals(TerracottaOperatorEvent.EVENT_TYPE.INFO.name())) {
      this.tcLogger.info(event.getEventSystem() + ":" + event.getEventMessage());
    } else if (eventType.equals(TerracottaOperatorEvent.EVENT_TYPE.WARN.name())) {
      this.tcLogger.warn(event.getEventSystem() + ":" + event.getEventMessage());
    } else if (eventType.equals(TerracottaOperatorEvent.EVENT_TYPE.DEBUG.name())) {
      this.tcLogger.debug(event.getEventSystem() + ":" + event.getEventMessage());
    } else if (eventType.equals(TerracottaOperatorEvent.EVENT_TYPE.ERROR.name())) {
      this.tcLogger.error(event.getEventSystem() + ":" + event.getEventMessage());
    } else {
      throw new RuntimeException("invalid event Type: " + eventType);
    }

  }

}
