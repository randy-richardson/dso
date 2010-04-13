/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;
import com.tc.tcevent.TCClusterEvent;

public interface TCClusterEventsMbean extends TerracottaMBean {
  void fireTCClusterEvent(TCClusterEvent tcEvent);
}