/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.tcevent.TerracottaSubSystemEvent;
import com.tc.tcevent.TerracottaSubSystemEventCallback;

import java.util.concurrent.CopyOnWriteArrayList;

public class TerracottaSubSystemEventLogger {
  
  private final CopyOnWriteArrayList<TerracottaSubSystemEventCallback> callbacks = new CopyOnWriteArrayList<TerracottaSubSystemEventCallback>();
  
  public void registerEventCallback(TerracottaSubSystemEventCallback tcEventCallback){
    this.callbacks.add(tcEventCallback);
  }
  
  public void fireEvent(TerracottaSubSystemEvent event){
    for(TerracottaSubSystemEventCallback callback : callbacks){
      callback.fireClusterEvent(event);
    }
  }
}
