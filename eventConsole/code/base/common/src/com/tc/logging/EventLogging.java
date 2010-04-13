/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class EventLogging {
  private static final EventLogger INSTANCE;
  
  static{
    INSTANCE = new EventLogger();
  }
  
  public static EventLogger getEventLogger(){
    return INSTANCE;
  }
}
