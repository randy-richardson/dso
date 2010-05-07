/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management;

import com.tc.operatorevent.TerracottaOperatorEvent;

public interface TerracottaOperatorEventsMBean extends TerracottaMBean {
  public static final String TERRACOTTA_OPERATOR_EVENT = "terracotta operator event";
  
  void fireOperatorEvent(TerracottaOperatorEvent tcEvent);
}