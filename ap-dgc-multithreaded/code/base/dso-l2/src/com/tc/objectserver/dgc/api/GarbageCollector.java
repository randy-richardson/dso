/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.dgc.api;

import com.tc.objectserver.dgc.impl.GCHook;
import com.tc.objectserver.dgc.impl.YoungGenChangeCollector;

public interface GarbageCollector extends BasicGarbageCollector {

  public void doGC(GCHook hook);

  public YoungGenChangeCollector getYoungGenChangeCollector();
  
}