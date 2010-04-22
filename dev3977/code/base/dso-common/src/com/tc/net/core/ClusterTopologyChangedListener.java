/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core;

public interface ClusterTopologyChangedListener {
  void serverAdded(ConnectionAddressProvider... addressProviders);
}
