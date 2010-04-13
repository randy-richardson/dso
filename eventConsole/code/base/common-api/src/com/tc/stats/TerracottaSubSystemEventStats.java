/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import com.tc.net.NodeID;

import java.util.Date;

public interface TerracottaSubSystemEventStats {
  
  NodeID getNodeID();

  String getTCEventType();
  
  Date getEventTime();
  
  String getSystem();
  
  String getMessage();

}
