/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.net.NodeIdProvider;
import com.tc.util.Assert;

public class TerracottaOperatorEventLogging {
  private static volatile NodeIdProvider nodeIdProvider;

  public static TerracottaOperatorEventLogger getEventLogger() {
    Assert.assertNotNull(nodeIdProvider);
    return TerracottaOperatorEventLoggerHolder.instance;
  }

  public static void setNodeIdProvider(NodeIdProvider nodeIDProvider) {
    if (nodeIdProvider == null) {
      nodeIdProvider = nodeIDProvider;
    }
    Assert.assertNotNull(nodeIdProvider);
  }

  private static class TerracottaOperatorEventLoggerHolder {
    static final TerracottaOperatorEventLogger instance = new TerracottaOperatorEventLogger(nodeIdProvider);
  }
}
