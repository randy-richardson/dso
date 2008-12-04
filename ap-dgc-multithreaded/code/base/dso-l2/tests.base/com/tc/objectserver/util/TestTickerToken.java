/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.util;

public class TestTickerToken implements TickerToken {
  
  private final boolean primary;
  private final int tick;
  
  public TestTickerToken(int tick) {
    this(false, tick);
  }
  
  public TestTickerToken(boolean primary, int tick) {
    this.primary = primary;
    this.tick = tick;
  }
  
  public boolean isPrimary() {
    return primary;
  }

  public int tick() {
    return tick;
  }

}
