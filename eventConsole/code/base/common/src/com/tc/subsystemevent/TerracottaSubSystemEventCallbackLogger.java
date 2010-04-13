/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.subsystemevent;

import com.tc.logging.TCLogger;
import com.tc.management.beans.TerracottaSubSystemEventsMbean;

public class TerracottaSubSystemEventCallbackLogger implements TerracottaSubSystemEventCallback{
  
  private final TCLogger tcLogger;
  private final TerracottaSubSystemEventsMbean tcSubSystemEventsMbean;
  
  public TerracottaSubSystemEventCallbackLogger(TCLogger tcLogger, TerracottaSubSystemEventsMbean tcSubSystemEventsMbean) {
    this.tcLogger = tcLogger;
    this.tcSubSystemEventsMbean = tcSubSystemEventsMbean;
  }

  public void fireClusterEvent(TerracottaSubSystemEvent event) {
    logEvent(event);
    this.tcSubSystemEventsMbean.fireTCClusterEvent(event);
  }

  private void logEvent(TerracottaSubSystemEvent event) {
    switch(event.getEventType()){
      case TerracottaSubSystemEvent.INFO:
        this.tcLogger.info(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      case TerracottaSubSystemEvent.WARN:
        this.tcLogger.warn(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      case TerracottaSubSystemEvent.DEBUG:
        this.tcLogger.debug(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      case TerracottaSubSystemEvent.ERROR:
        this.tcLogger.error(event.getEventSystem() + ":" + event.getEventMessage());
        break;
      default:
        throw new RuntimeException("invalid event type");
    }
    
  }

}
