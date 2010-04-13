/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.core.ConnectionAddressProvider;

public interface ReloadConfig {
  public void reloadConfig(ConnectionAddressProvider cap);
}
