/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.test.activepassive.MultipleServerTestSetupManager;
import com.tc.test.activepassive.ServerDataShareMode;
import com.tc.test.activepassive.ServerPersistenceMode;

public class LinkedBlockingQueueActiveActiveTest extends TransparentTestBase {

  private static final int NODE_COUNT = 1;

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueCrashTestApp.class;
  }

  protected boolean canRunActiveActive() {
    return true;
  }

  public void setupActiveActiveTest(MultipleServerTestSetupManager setupManager) {
    setupManager.setServerCount(2);
    setupManager.setServerShareDataMode(ServerDataShareMode.DISK);
    setupManager.setServerPersistenceMode(ServerPersistenceMode.PERMANENT_STORE);
    setupManager.addActiveServerGroup(1, ServerDataShareMode.DISK);
    setupManager.addActiveServerGroup(1, ServerDataShareMode.DISK);
  }

}
