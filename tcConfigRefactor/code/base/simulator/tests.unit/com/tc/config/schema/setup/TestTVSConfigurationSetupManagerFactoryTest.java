/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.object.config.schema.AutoLock;
import com.tc.object.config.schema.Lock;
import com.tc.object.config.schema.LockLevel;
import com.tc.test.TCTestCase;

import java.io.File;

/**
 * Unit test for {@link TestTVSConfigurationSetupManagerFactory}. Because that class builds up a whole config system,
 * this test actually stresses a large swath of the configuration system.
 */
public class TestTVSConfigurationSetupManagerFactoryTest extends TCTestCase {

  private TestTVSConfigurationSetupManagerFactory factory;
  private L2TVSConfigurationSetupManager          l2Manager;
  private L1TVSConfigurationSetupManager          l1Manager;

  public TestTVSConfigurationSetupManagerFactoryTest() {
    // this.disableAllUntil(new Date(Long.MAX_VALUE));
  }

  @Override
  public void setUp() throws Exception {
    this.factory = new TestTVSConfigurationSetupManagerFactory(
                                                               TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                               null, new FatalIllegalConfigurationChangeHandler());

    this.factory.l2CommonConfig().setLogsPath(getTempFile("l2-logs").toString());
    this.factory.l1CommonConfig().setLogsPath(getTempFile("l1-logs").toString());

    this.l2Manager = this.factory.createL2TVSConfigurationSetupManager(null);
    this.l1Manager = this.factory.getL1TVSConfigurationSetupManager();
  }

  public void testSettingValues() throws Exception {
    // A string array value
    this.l1Manager.dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME)
        .transientFields().setFieldNameArray(new String[] { "Foo.foo", "Bar.bar" });

    // Hit the remaining top-level config objects
    this.l2Manager.dsoL2Config().garbageCollection().setInterval(142);
    this.l1Manager.commonL1Config().setLogsPath("whatever");
    this.l2Manager.commonl2Config().setDataPath("marph");

    // A complex value (locks)
    this.l1Manager.dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME)
        .setLocks(
                  new Lock[] { new AutoLock("* Foo.foo(..)", LockLevel.CONCURRENT),
                      new com.tc.object.config.schema.NamedLock("bar", "* Baz.baz(..)", LockLevel.READ) });

    // A sub-config object
    this.l1Manager.dsoL1Config().instrumentationLoggingOptions().setLogDistributedMethods(true);

    this.factory.activateConfigurationChange();

    System.err.println(this.l2Manager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME));
    System.err.println(this.l2Manager.systemConfig());
    System.err.println(this.l1Manager.dsoL1Config());

    assertEquals(142, this.l2Manager.dsoL2Config().garbageCollection().getInterval());
    assertEquals(new File("whatever"), this.l1Manager.commonL1Config().logsPath());
    assertEquals(new File("marph"), this.l2Manager.commonl2Config().dataPath());
    assertEqualsUnordered(new Lock[] { new AutoLock("* Foo.foo(..)", LockLevel.CONCURRENT),
        new com.tc.object.config.schema.NamedLock("bar", "* Baz.baz(..)", LockLevel.READ) }, this.l1Manager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME).locks());
    assertTrue(this.l1Manager.dsoL1Config().instrumentationLoggingOptions().logDistributedMethods());
  }

}
