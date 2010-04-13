/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.tcevent.TCClusterEvent;
import com.tc.tcevent.TCEventCallback;

import java.util.concurrent.CopyOnWriteArrayList;

public class EventLogger {
  
  private final CopyOnWriteArrayList<TCEventCallback> callbacks = new CopyOnWriteArrayList<TCEventCallback>();
  
  public void registerEventCallback(TCEventCallback tcEventCallback){
    this.callbacks.add(tcEventCallback);
  }
  
  public void fireEvent(TCClusterEvent event){
    for(TCEventCallback callback : callbacks){
      callback.fireClusterEvent(event);
    }
  }
}
