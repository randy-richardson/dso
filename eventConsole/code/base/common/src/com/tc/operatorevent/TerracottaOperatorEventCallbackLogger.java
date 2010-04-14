/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.management.beans.TerracottaOperatorEventsMbean;

public class TerracottaOperatorEventCallbackLogger implements TerracottaOperatorEventCallback{
  
  private final TCLogger tcLogger;
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
    switch(event.getEventType()){
      case TerracottaOperatorEvent.INFO:
        this.tcLogger.info(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      case TerracottaOperatorEvent.WARN:
        this.tcLogger.warn(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      case TerracottaOperatorEvent.DEBUG:
        this.tcLogger.debug(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      case TerracottaOperatorEvent.ERROR:
        this.tcLogger.error(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      default:
        throw new RuntimeException("invalid event type");
    }
    
  }

}
