/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.management.criticalevents.CriticalEvent;

public interface L1CriticalEventsMbean extends TerracottaMBean {
  void fireL1CriticalEvent(CriticalEvent tcEvent);
}