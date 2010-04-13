/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.tcevent;


public interface TCEventCallback {
  void fireClusterEvent(TCClusterEvent event);
}
