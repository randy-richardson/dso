/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;

public class TerracottaOperatorEventCallbackLogger {

  private final TCLogger logger;

  public TerracottaOperatorEventCallbackLogger(TCLogger logger) {
    this.logger = logger;
  }

  public void logEvent(TerracottaOperatorEvent event) {
    EventType eventType = event.getEventType();
    switch (eventType) {
      case INFO:
        this.logger.info(event.getNodeId() + " " + event.getEventSubsystem() + " " + event.getEventMessage());
        break;
      case WARN:
        this.logger.warn(event.getNodeId() + " " + event.getEventSubsystem() + " " + event.getEventMessage());
        break;
      case DEBUG:
        this.logger.debug(event.getNodeId() + " " + event.getEventSubsystem() + " " + event.getEventMessage());
        break;
      case ERROR:
        this.logger.error(event.getNodeId() + " " + event.getEventSubsystem() + " " + event.getEventMessage());
        break;
      default:
        throw new RuntimeException("Invalid Event Type: " + eventType);
    }
  }
}
