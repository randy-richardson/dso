/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.subsystemevent.TerracottaSubSystemEvent;

public interface TerracottaSubSystemEventsMbean extends TerracottaMBean {
  void fireTCClusterEvent(TerracottaSubSystemEvent tcEvent);
}