/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.subsystemevent;

import java.io.Serializable;


public interface TerracottaSubSystemEvent extends Serializable {
  public static final int INFO = 0;
  public static final int WARN = 1;
  public static final int DEBUG = 2;
  public static final int ERROR = 3;
  
  public static final String LONG_GC = "LONG_GC";
  
  String getEventTime();
  
  String getNodeId();

  int getEventType();

  String getEventSystem();
  
  String getEventMessage();
  
}
