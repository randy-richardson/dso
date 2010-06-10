/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.util.ResourceBundle;

class TerracottaOperatorEventResources {
  private static final TerracottaOperatorEventResources instance;
  private final ResourceBundle resources;
  static {
    instance = new TerracottaOperatorEventResources();
  }

  private TerracottaOperatorEventResources() {
    this.resources = ResourceBundle.getBundle(getClass().getPackage().getName() + ".operatorevents");
  }
  
  static String getLongGCMessage() {
    return instance.resources.getString("long.gc");
  }
  
  static String getDGCStartedMessage() {
    return instance.resources.getString("dgc.started");
  }

  static String getDGCFinishedMessage() {
    return instance.resources.getString("dgc.finished");
  }

  static String getDGCCanceledMessage() {
    return instance.resources.getString("dgc.canceled");
  }
  
  static String getNodeAvailabiltyMessage() {
    return instance.resources.getString("node.availability");
  }

  public static String getLockGCMessage() {
    return instance.resources.getString("lock.gc");
  }

  public static String getHighMemoryUsageMessage() {
    return instance.resources.getString("high.memory.usage");
  }

  public static String getOOODisconnectMessage() {
    return instance.resources.getString("ooo.disconnect");
  }

  public static String getOOOConnectMessage() {
    return instance.resources.getString("ooo.connect");
  }

  public static String getClusterNodeStateChangedMessage() {
    return instance.resources.getString("node.state");
  }

  public static String getZapRequestReceivedMessage() {
    return instance.resources.getString("zap.received");
  }
  
  public static String getZapRequestAcceptedMessage() {
    return instance.resources.getString("zap.accepted");
  }
}

