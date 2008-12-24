/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class TickerTokenHandlerDelegate {

  private final AtomicBoolean dirty = new AtomicBoolean(true);
  
  private final AtomicBoolean acceptTokenReader = new AtomicBoolean(false);
  
  public void makeDirty() {
    dirty.set(true);
  }
  
  public boolean isDirtyAndClear() {
    return dirty.getAndSet(false);
  }

  public void acceptTokenReader() {
    acceptTokenReader.set(true);
  }
  
  public void unacceptTokenReader() {
    acceptTokenReader.set(false);
  }
  
  public boolean allowTokenReader() {
    return acceptTokenReader.get();
  }
}
