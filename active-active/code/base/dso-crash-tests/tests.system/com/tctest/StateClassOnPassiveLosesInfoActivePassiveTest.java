/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ServerCrashMode;
import com.tc.test.activepassive.ServerPersistenceMode;
import com.tc.test.activepassive.ServerDataShareMode;
import com.tc.test.activepassive.MultipleServerTestSetupManager;

public class StateClassOnPassiveLosesInfoActivePassiveTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT).setIntensity(1);
    t.initializeTestRunner();
  }

  @SuppressWarnings("unchecked")
  protected Class getApplicationClass() {
    return StateClassOnPassiveLosesInfoActivePassiveTestApp.class;
  }

  protected boolean canRunCrash() {
    return true;
  }

  protected boolean canRunActivePassive() {
    return true;
  }

  public void setupActivePassiveTest(MultipleServerTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerCrashMode(ServerCrashMode.CONTINUOUS_ACTIVE_CRASH);
    setupManager.setServerCrashWaitTimeInSec(30);
    setupManager.setServerShareDataMode(ServerDataShareMode.NETWORK);
    setupManager.setServerPersistenceMode(ServerPersistenceMode.TEMPORARY_SWAP_ONLY);
  }

}
