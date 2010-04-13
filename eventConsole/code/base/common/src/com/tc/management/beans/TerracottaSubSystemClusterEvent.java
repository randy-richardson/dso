/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.logging.TerracottaSubSystemEventLogger;
import com.tc.logging.TerracottaSubSystemEventLogging;
import com.tc.stats.AbstractNotifyingMBean;
import com.tc.subsystemevent.TerracottaSubSystemEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;

public class TerracottaSubSystemClusterEvent extends AbstractNotifyingMBean implements TerracottaSubSystemEventsMbean, NotificationListener{
  
  private TerracottaSubSystemEventLogger tcEventLogger = TerracottaSubSystemEventLogging.getEventLogger();

  public TerracottaSubSystemClusterEvent() throws NotCompliantMBeanException {
    super(TerracottaSubSystemEventsMbean.class);
  }

  public void fireTCClusterEvent(TerracottaSubSystemEvent tcEvent) {
    sendNotification(tcEvent.getEventSystem(), tcEvent);
  }

  public void reset() {
    //
  }

  public void handleNotification(Notification notification, Object handback) {
    tcEventLogger.fireTCSubSystemEvent((TerracottaSubSystemEvent) notification.getSource());
  }

}
