/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.util;


/**
 * This class is a token that should passed around and increment it.
 */
public interface TickerToken {
   
  public boolean isPrimary();
  
  public int tick();
}
