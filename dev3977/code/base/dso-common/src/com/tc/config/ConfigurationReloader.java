/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.core.ConnectionAddressProvider;

public interface ConfigurationReloader {
  public void reloadConfiguration(ConnectionAddressProvider... cap);
}
