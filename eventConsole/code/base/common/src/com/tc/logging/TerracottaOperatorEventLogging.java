/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

public class TerracottaOperatorEventLogging {
  private static final TerracottaOperatorEventLogger INSTANCE = new TerracottaOperatorEventLogger();
  
  public static TerracottaOperatorEventLogger getEventLogger(){
    return INSTANCE;
  }
}
