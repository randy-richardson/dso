/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.stats.AbstractNotifyingMBean;
import com.tc.tcevent.TCClusterEvent;

import javax.management.NotCompliantMBeanException;

public class ClusterEvent extends AbstractNotifyingMBean implements TCClusterEventsMbean{

  public ClusterEvent() throws NotCompliantMBeanException {
    super(TCClusterEventsMbean.class);
  }

  public void fireTCClusterEvent(TCClusterEvent tcEvent) {
    sendNotification(tcEvent.getEventName(), this);
  }

  public void reset() {
    //
  }

}
