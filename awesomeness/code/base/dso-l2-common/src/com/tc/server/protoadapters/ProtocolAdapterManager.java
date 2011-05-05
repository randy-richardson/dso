/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server.protoadapters;

public class ProtocolAdapterManager {

  private final GlobalStorageManager globalStorageManager;

  public ProtocolAdapterManager() {
    globalStorageManager = new GlobalStorageManager();
  }

  public void start() {
    globalStorageManager.start();
  }

  public GlobalStorageManager getGlobalStorageManager() {
    return globalStorageManager;
  }

}
