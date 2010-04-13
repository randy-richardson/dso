/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class TerracottaSubSystemEventLogging {
  private static final TerracottaSubSystemEventLogger INSTANCE = new TerracottaSubSystemEventLogger();
  
  public static TerracottaSubSystemEventLogger getEventLogger(){
    return INSTANCE;
  }
}
