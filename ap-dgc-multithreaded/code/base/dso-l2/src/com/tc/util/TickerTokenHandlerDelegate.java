/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class TickerTokenHandlerDelegate {
  
  private CollectContext collectContext = new CollectContext();
  
  
  private final AtomicBoolean dirty = new AtomicBoolean(true);
  
  public void makeDirty() {
    dirty.set(true);
  }
  
  public boolean isDirty() {
    return dirty.get();
  }
  
  public void clean() {
    dirty.set(false);
  }
  
  public CollectContext getCollectContext() {
    collectContext.collect(TickerToken.DIRTY_STATE, dirty.get());
    return collectContext;
    
  }
}
