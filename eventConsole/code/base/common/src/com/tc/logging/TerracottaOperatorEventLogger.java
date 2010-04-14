/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;

import java.util.concurrent.CopyOnWriteArrayList;

public class TerracottaOperatorEventLogger {
  
  private final CopyOnWriteArrayList<TerracottaOperatorEventCallback> callbacks = new CopyOnWriteArrayList<TerracottaOperatorEventCallback>();
  
  public void registerEventCallback(TerracottaOperatorEventCallback tcEventCallback){
    this.callbacks.add(tcEventCallback);
  }
  
  public void fireOperatorEvent(TerracottaOperatorEvent event){
    for(TerracottaOperatorEventCallback callback : callbacks){
      callback.fireOperatorEvent(event);
    }
  }
}
