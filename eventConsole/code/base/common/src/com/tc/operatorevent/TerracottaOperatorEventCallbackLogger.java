/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.management.beans.TerracottaOperatorEventsMbean;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

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
    EventType eventType = event.getEventType();
    switch (eventType) {
      case INFO:
        this.tcLogger.info(event.getEventSubSystem() + ":" + event.getEventMessage());
        break;
      case WARN:
        this.tcLogger.warn(event.getEventSubSystem() + ":" + event.getEventMessage());
        break;
      case DEBUG:
        this.tcLogger.debug(event.getEventSubSystem() + ":" + event.getEventMessage());
        break;
      case ERROR:
        this.tcLogger.error(event.getEventSubSystem() + ":" + event.getEventMessage());
        break;
      default:
        throw new RuntimeException("invalid event Type: " + eventType);
    }
  }

}
