/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.net.NodeIdProvider;
import com.tc.util.Assert;

public class TerracottaOperatorEventLogging {
  private static volatile TerracottaOperatorEventLogger instance;
  private static volatile NodeIdProvider                nodeIdProvider;

  public static TerracottaOperatorEventLogger getEventLogger() {
    if (instance == null) {
      synchronized (TerracottaOperatorEventLogging.class) {
        if (instance == null) {
          Assert.assertNotNull(nodeIdProvider);
          instance = new TerracottaOperatorEventLogger(nodeIdProvider);
        }
      }
    }

    return instance;
  }

  public static void setNodeIdProvider(NodeIdProvider nodeIDProvider) {
    if (nodeIdProvider == null) {
      nodeIdProvider = nodeIDProvider;
    }
    Assert.assertNotNull(nodeIdProvider);
  }
}
