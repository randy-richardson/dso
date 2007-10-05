/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.ServerCrashMode;
import com.tc.test.activepassive.ServerPersistenceMode;
import com.tc.test.activepassive.ServerDataShareMode;
import com.tc.test.activepassive.MultipleServerTestSetupManager;
import com.tctest.runner.TransparentAppConfig;

public class ReentrantLockCrashTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public void doSetUp(TransparentTestIface t) throws Exception {
    TransparentAppConfig appConfig = t.getTransparentAppConfig();
    appConfig.setClientCount(NODE_COUNT);
    t.initializeTestRunner();
    appConfig.setAttribute(ReentrantLockTestApp.CRASH_TEST, "true");
  }

  protected Class getApplicationClass() {
    return ReentrantLockTestApp.class;
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
