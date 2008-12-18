/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class TallyHandlerDelegate implements TallyHandler {

  private AtomicBoolean dirty = new AtomicBoolean(false);
  
  public void makeDirty() {
    dirty.set(true);
  }
  
  public boolean isDirtyAndClear() {
    return dirty.getAndSet(false);
  }

}
