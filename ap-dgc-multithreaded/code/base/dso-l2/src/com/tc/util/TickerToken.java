/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCSerializable;


/**
 * this interface is to collect all tokens and update these content.
 */
public interface TickerToken extends TCSerializable {
  
  public static final String DIRTY_STATE = "dirty_state";

  public int getPrimaryID();
  
  public int getPrimaryTickValue();

  public void collectToken(int aId, CollectContext context);
  
  public boolean evaluateComplete();

 
}
