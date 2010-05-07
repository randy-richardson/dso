/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;


public interface TerracottaOperatorEventStats {
  
  String getNodeId();

  String getEventType();
  
  String getEventTime();
  
  String getEventSubsystem();
  
  String getEventMessage();

}
