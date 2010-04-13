/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.tcevent;


public interface TerracottaSubSystemEventCallback {
  void fireClusterEvent(TerracottaSubSystemEvent event);
}
