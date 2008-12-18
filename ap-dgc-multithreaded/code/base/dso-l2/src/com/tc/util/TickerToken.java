/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.Map;

/**
 * this interface is to collect all tokens and update these content.
 */
public interface TickerToken {

  public int getPrimaryID();
  
  public int getPrimaryTickValue();

  public void collectToken(int aId, boolean dirtyState);
  
  public Map<Integer, Boolean> getTokenStateMap();

 
}
