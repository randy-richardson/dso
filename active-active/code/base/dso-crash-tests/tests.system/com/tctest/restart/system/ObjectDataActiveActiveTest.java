/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.restart.system;

import com.tc.test.activepassive.MultipleServerTestSetupManager;
import com.tc.test.activepassive.ServerDataShareMode;
import com.tc.test.activepassive.ServerPersistenceMode;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

public class ObjectDataActiveActiveTest extends TransparentTestBase implements TestConfigurator {

  private int clientCount = 1;

  protected Class<ObjectDataTestApp> getApplicationClass() {
    return ObjectDataTestApp.class;
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(clientCount).setIntensity(1);
    t.initializeTestRunner();
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
