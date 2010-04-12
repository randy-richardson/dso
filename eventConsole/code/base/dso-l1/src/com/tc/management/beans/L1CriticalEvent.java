/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.criticalevents.CriticalEvent;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

public class L1CriticalEvent extends AbstractNotifyingMBean implements L1CriticalEventsMbean{

  public L1CriticalEvent() throws NotCompliantMBeanException {
    super(L1CriticalEventsMbean.class);
  }

  public void fireL1CriticalEvent(CriticalEvent tcEvent) {
    sendNotification(tcEvent.getEventName(), this);
  }

  public void reset() {
    //
  }

}
