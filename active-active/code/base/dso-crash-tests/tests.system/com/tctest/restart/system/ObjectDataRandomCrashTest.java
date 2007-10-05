/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.activepassive.ServerCrashMode;
import com.tc.test.activepassive.ServerPersistenceMode;
import com.tc.test.activepassive.ServerDataShareMode;
import com.tc.test.activepassive.MultipleServerTestSetupManager;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ObjectDataRandomCrashTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 2;

  protected Class getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
  }

  protected boolean canRunActivePassive() {
    return true;
  }
  
  public void setupActivePassiveTest(MultipleServerTestSetupManager setupManager) {
    setupManager.setServerCount(3);
    setupManager.setServerCrashMode(ServerCrashMode.RANDOM_SERVER_CRASH);
    setupManager.setServerCrashWaitTimeInSec(20);
    setupManager.setServerShareDataMode(ServerDataShareMode.NETWORK);
    setupManager.setServerPersistenceMode(ServerPersistenceMode.TEMPORARY_SWAP_ONLY);
  }
}