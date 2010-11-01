/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.object.config.schema.NewL2DSOConfigObject;
import com.terracottatech.config.Server;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;

/**
 * Unit/subsystem test for {@link NewCommonL2ConfigObject}.
 */
public class NewCommonL2ConfigObjectTest extends ConfigObjectTestBase {

  private NewCommonL2ConfigObject object;

  @Override
  public void setUp() throws Exception {
    TcConfig config = TcConfig.Factory.newInstance();
    super.setUp(Server.class);
    NewL2DSOConfigObject.initializeServers(config, new SchemaDefaultValueProvider(), getTempDirectory());
    setBean(config.getServers().getServerArray(0));
    System.out.println("XXXX " + context().bean());
    this.object = new NewCommonL2ConfigObject(context());
  }

  @Override
  protected XmlObject getBeanFromTcConfig(TcConfig domainConfig) throws Exception {
    return domainConfig.getServers().getServerArray(0);
  }

  public void xtestConstruction() throws Exception {
    try {
      new NewCommonL2ConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testDataPath() throws Exception {

    assertEquals(new File(getTempDirectory(), "data"), object.dataPath());
    checkNoListener();

    builder().getServers().getL2s()[0].setData("foobar");
    setConfig();

    assertEquals(new File(getTempDirectory(), "foobar"), object.dataPath());
    checkListener(new File("data"), new File("foobar"));
  }

  public void xtestLogsPath() throws Exception {
    // addListeners(object.logsPath());

    assertEquals(new File("logs"), object.logsPath());
    checkNoListener();

    builder().getServers().getL2s()[0].setLogs("foobar");
    setConfig();

    assertEquals(new File("foobar"), object.logsPath());
    checkListener(new File("logs"), new File("foobar"));
  }

  public void xtestJmxPort() throws Exception {
    // addListeners(object.jmxPort());

    assertEquals(9520, object.jmxPort());
    checkNoListener();

    builder().getServers().getL2s()[0].setJMXPort(3285);
    setConfig();

    assertEquals(3285, object.jmxPort());
  }

}
