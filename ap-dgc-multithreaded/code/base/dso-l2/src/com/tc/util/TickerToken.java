/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.HashMap;
import java.util.Map;


/**
 * this interface is to collect all tokens and update these content.
 */
public class TickerToken {
  
  protected final Map<Integer,Boolean> tokenStateMap = new HashMap();
  protected final int id;
  
  public TickerToken(int id) {
    this.id = id;
  }
  
  public int getID() {
    return id;
  }
  
  public void collectToken(int aId, boolean dirtyState) {
    tokenStateMap.put(aId, dirtyState);
  }
  
  public Map<Integer,Boolean> getTokenStateMap() {
    return tokenStateMap;
  }
  
  
}
