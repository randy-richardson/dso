/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;


public interface TerracottaOperatorEventStats {
  
  String getNodeID();

  String getTCEventType();
  
  String getEventTime();
  
  String getSystem();
  
  String getMessage();

}
