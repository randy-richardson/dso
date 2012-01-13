/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.object.config.schema.L1DSOConfig;

/**
 * Knows how to set up configuration for L1.
 */
public interface L1ConfigurationSetupManager {
  String[] processArguments();

  boolean loadedFromTrustedSource();

  String rawConfigText();

  CommonL1Config commonL1Config();

  L2ConfigForL1 l2Config();

  L1DSOConfig dsoL1Config();

  void setupLogging();

  String[] applicationNames();

  void reloadServersConfiguration() throws ConfigurationSetupException;
}
