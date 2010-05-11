/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import com.tc.logging.TCLogger;
import com.tc.management.TerracottaOperatorEventsMBean;

public class TerracottaOperatorEventCallbackNotifier implements TerracottaOperatorEventCallback {

  private final TerracottaOperatorEventCallbackLogger tcCallbackLogger;
  private final TerracottaOperatorEventsMBean         tcOperatorEventsMbean;

  public TerracottaOperatorEventCallbackNotifier(TCLogger tcLogger, TerracottaOperatorEventsMBean tcOperatorEventsMbean) {
    this.tcCallbackLogger = new TerracottaOperatorEventCallbackLogger(tcLogger);
    this.tcOperatorEventsMbean = tcOperatorEventsMbean;
  }

  public void fireOperatorEvent(TerracottaOperatorEvent event) {
    tcCallbackLogger.logEvent(event);
    this.tcOperatorEventsMbean.fireOperatorEvent(event);
  }

}
