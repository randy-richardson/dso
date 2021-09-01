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

import org.junit.Test;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.security.PwProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class StandardConfigurationSetupManagerFactoryTest {

  @Test
  public void testSafeModeConfiguration() throws Exception {
    String[] args = {"-f", this.getClass().getResource("/default-config.xml").getPath(), "--safe-mode"};

    StandardConfigurationSetupManagerFactory factory =
        new StandardConfigurationSetupManagerFactory(args,
                                                     StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                     mock(IllegalConfigurationChangeHandler.class),
                                                     mock(PwProvider.class));

    L2ConfigurationSetupManager l2TVSConfigurationSetupManager =
        factory.createL2TVSConfigurationSetupManager(null, false);

    assertTrue(l2TVSConfigurationSetupManager.isSafeModeConfigured());
  }

  @Test
  public void testNonSafeModeConfiguration() throws Exception {
    String[] args = {"-f", this.getClass().getResource("/default-config.xml").getPath()};

    StandardConfigurationSetupManagerFactory factory =
        new StandardConfigurationSetupManagerFactory(args,
                                                     StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                     mock(IllegalConfigurationChangeHandler.class),
                                                     mock(PwProvider.class));

    L2ConfigurationSetupManager l2TVSConfigurationSetupManager =
        factory.createL2TVSConfigurationSetupManager(null, false);

    assertFalse(l2TVSConfigurationSetupManager.isSafeModeConfigured());
  }
}