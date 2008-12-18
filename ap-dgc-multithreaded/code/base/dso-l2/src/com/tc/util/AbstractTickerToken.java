/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTickerToken implements TickerToken {

  protected final Map<Integer, Boolean> tokenStateMap;
  protected final int                   primaryID;
  protected final int                   primaryTickValue;

  public AbstractTickerToken(int primaryID, int primaryTickValue) {
    this(primaryID, primaryTickValue, new HashMap<Integer, Boolean>());
  }

  public AbstractTickerToken(int primaryID, int primaryTickValue, Map<Integer, Boolean> tokenStateMap) {
    this.primaryID = primaryID;
    this.primaryTickValue = primaryTickValue;
    this.tokenStateMap = tokenStateMap;
  }

  public int getPrimaryID() {
    return primaryID;
  }

  public int getPrimaryTickValue() {
    return primaryTickValue;
  }

  public void collectToken(int aId, boolean dirtyState) {
    tokenStateMap.put(aId, dirtyState);
  }

  public Map<Integer, Boolean> getTokenStateMap() {
    return tokenStateMap;
  }

}
