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
  private final String                  thisNodeId;

  public TerracottaOperatorClusterEvent(String thisNodeId) throws NotCompliantMBeanException {
    super(TerracottaOperatorEventsMbean.class);
    this.thisNodeId = thisNodeId;
  }

  public void fireOperatorEvent(TerracottaOperatorEvent tcEvent) {
    // TODO: should be better mechanism to set server's node id
    if (tcEvent.getNodeId() == null) {
      tcEvent.setNodeId(thisNodeId);
    }
    sendNotification(tcEvent.getEventSystem(), tcEvent);
  }

  public void reset() {
    //
  }

  public void handleNotification(Notification notification, Object handback) {
    TerracottaOperatorEvent tcOperatorEvent = (TerracottaOperatorEvent) notification.getSource();
    tcOperatorEvent.setNodeId((String) handback);
    tcEventLogger.fireOperatorEvent((TerracottaOperatorEvent) notification.getSource());
  }

}
