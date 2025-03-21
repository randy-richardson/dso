/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.config.schema.test;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.CommonL1ConfigObject;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.object.config.schema.L1DSOConfigObject;
import com.terracottatech.config.Client;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.net.InetAddress;

/**
 * Unit/subsystem test for {@link CommonL1ConfigObject}.
 */
public class CommonL1ConfigObjectTest extends ConfigObjectTestBase {

  private CommonL1ConfigObject object;

  @Override
  public void setUp() throws Exception {
    TcConfig config = TcConfig.Factory.newInstance();
    super.setUp(Client.class);
    L1DSOConfigObject.initializeClients(config, new SchemaDefaultValueProvider());
    setBean(config.getClients());
    this.object = new CommonL1ConfigObject(context());
  }

  @Override
  protected XmlObject getBeanFromTcConfig(TcConfig config) throws Exception {
    return config.getClients();
  }

  public void testConstruction() throws Exception {
    try {
      new CommonL1ConfigObject(null);
      fail("Didn't get NPE on no context");
    } catch (NullPointerException npe) {
      // ok
    }
  }

  public void testLogsPath() throws Exception {

    assertEquals(new File(getTempDirectory().getParent(), "logs-" + InetAddress.getLocalHost().getHostAddress()),
                 object.logsPath());
    checkNoListener();

    Client client = (Client) context().bean();
    client.setLogs("foobar");

    assertEquals(new File("foobar"), object.logsPath());
  }

}
