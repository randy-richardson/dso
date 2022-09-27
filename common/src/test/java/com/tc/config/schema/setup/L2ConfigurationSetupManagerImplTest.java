/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.config.schema.setup;

import org.junit.BeforeClass;
import org.junit.Test;


import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.junit.Assert.fail;

/**
 * Created by alsu on 16/03/16.
 */
public class L2ConfigurationSetupManagerImplTest {
  private static TestConfigurationSetupManagerFactory factory;

  @BeforeClass
  public static void oneTimeSetup() throws ConfigurationSetupException {
    factory = new TestConfigurationSetupManagerFactory(new FatalIllegalConfigurationChangeHandler());
  }

  @Test
  public void testFailoverPriorityGroupMemberCountValidation() throws Exception {
    File tcConfig = new File(this.getClass().getClassLoader().getResource("failover-priority-config.xml").toURI());
    try {
      factory.createL2TVSConfigurationSetupManager(tcConfig, "server1");
      fail("L2ConfigurationSetupManager instantiation should have failed");
    } catch (ConfigurationSetupException cse) {
      assertThat(cse.getMessage(),
          containsString("group1" + " contains more than two servers " +
                         "which is a violation of the CONSISTENCY failover priority requirement"));
    }
  }

}