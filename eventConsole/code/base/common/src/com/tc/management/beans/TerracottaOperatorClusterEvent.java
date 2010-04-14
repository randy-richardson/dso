/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;

public class TerracottaOperatorClusterEvent extends AbstractNotifyingMBean implements TerracottaOperatorEventsMbean, NotificationListener{
  
  private TerracottaOperatorEventLogger tcEventLogger = TerracottaOperatorEventLogging.getEventLogger();

  public TerracottaOperatorClusterEvent() throws NotCompliantMBeanException {
    super(TerracottaOperatorEventsMbean.class);
  }

  public void fireOperatorEvent(TerracottaOperatorEvent tcEvent) {
    sendNotification(tcEvent.getEventSystem(), tcEvent);
  }

  public void reset() {
    //
  }

  public void handleNotification(Notification notification, Object handback) {
    tcEventLogger.fireOperatorEvent((TerracottaOperatorEvent) notification.getSource());
  }

}
