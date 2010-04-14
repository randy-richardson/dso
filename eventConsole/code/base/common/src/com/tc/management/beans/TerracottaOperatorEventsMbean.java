/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.operatorevent.TerracottaOperatorEvent;

public interface TerracottaOperatorEventsMbean extends TerracottaMBean {
  void fireOperatorEvent(TerracottaOperatorEvent tcEvent);
}