/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.subsystemevent;


public interface TerracottaSubSystemEventCallback {
  void fireClusterEvent(TerracottaSubSystemEvent event);
}
