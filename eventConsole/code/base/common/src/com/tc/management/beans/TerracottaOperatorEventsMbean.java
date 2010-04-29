/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;

import javax.management.NotificationListener;

public interface TerracottaOperatorEventsMbean extends TerracottaMBean, NotificationListener {
  void fireOperatorEvent(TerracottaOperatorEvent tcEvent);
}