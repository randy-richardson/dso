/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.stats.AbstractNotifyingMBean;
import com.tc.tcevent.TerracottaSubSystemEvent;

import javax.management.NotCompliantMBeanException;

public class TerracottaSubSystemClusterEvent extends AbstractNotifyingMBean implements TerracottaSubSystemEventsMbean{

  public TerracottaSubSystemClusterEvent() throws NotCompliantMBeanException {
    super(TerracottaSubSystemEventsMbean.class);
  }

  public void fireTCClusterEvent(TerracottaSubSystemEvent tcEvent) {
    sendNotification(tcEvent.getEventName(), this);
  }

  public void reset() {
    //
  }

}
