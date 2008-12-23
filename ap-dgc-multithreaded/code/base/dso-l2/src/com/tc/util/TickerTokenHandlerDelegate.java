/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class TickerTokenHandlerDelegate {

  private final AtomicBoolean dirty = new AtomicBoolean(true);
  
  public void makeDirty() {
    dirty.set(true);
  }
  
  public boolean isDirtyAndClear() {
    return dirty.getAndSet(false);
  }

}
