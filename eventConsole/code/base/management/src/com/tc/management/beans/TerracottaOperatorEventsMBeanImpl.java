/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaOperatorEventsMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.stats.AbstractNotifyingMBean;

import javax.management.NotCompliantMBeanException;

public class TerracottaOperatorEventsMBeanImpl extends AbstractNotifyingMBean implements TerracottaOperatorEventsMBean {

  public TerracottaOperatorEventsMBeanImpl() throws NotCompliantMBeanException {
    super(TerracottaOperatorEventsMBean.class);
  }

  public void fireOperatorEvent(TerracottaOperatorEvent tcEvent) {
    sendNotification(TERRACOTTA_OPERATOR_EVENT, tcEvent);
  }

  public void reset() {
    //
  }

  public void logOperatorEvent(TerracottaOperatorEvent event) {
    fireOperatorEvent(event);
  }


}
