/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.net.NodeNameProvider;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;

import java.util.concurrent.CopyOnWriteArrayList;

public class TerracottaOperatorEventLogger {

  private final CopyOnWriteArrayList<TerracottaOperatorEventCallback> callbacks = new CopyOnWriteArrayList<TerracottaOperatorEventCallback>();
  private final NodeNameProvider                                      nodeNameProvider;

  public TerracottaOperatorEventLogger(NodeNameProvider nodeIdProvider) {
    this.nodeNameProvider = nodeIdProvider;
  }

  public void registerEventCallback(TerracottaOperatorEventCallback callback) {
    this.callbacks.add(callback);
  }

  public void fireOperatorEvent(TerracottaOperatorEvent event) {
    event.setNodeName(this.nodeNameProvider.getNodeName());
    for (TerracottaOperatorEventCallback callback : callbacks) {
      callback.logOperatorEvent(event);
    }
  }
}
