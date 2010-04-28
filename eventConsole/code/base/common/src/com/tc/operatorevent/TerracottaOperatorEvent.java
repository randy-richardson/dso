/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.operatorevent;

import java.io.Serializable;


public interface TerracottaOperatorEvent extends Serializable {
  public static final int INFO = 0;
  public static final int WARN = 1;
  public static final int DEBUG = 2;
  public static final int ERROR = 3;
  
  public static final String LONG_GC = "LONG_GC";
  
  String getEventTime();
  
  String getNodeId();
  
  void setNodeId(String nodeId);

  int getEventType();

  String getEventSystem();
  
  String getEventMessage();
  
}
