/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.NodeIdProvider;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventCallback;

import java.util.concurrent.CopyOnWriteArrayList;

public class TerracottaOperatorEventLogger {

  private final CopyOnWriteArrayList<TerracottaOperatorEventCallback> callbacks = new CopyOnWriteArrayList<TerracottaOperatorEventCallback>();
  private NodeIdProvider                                              nodeIdProvider;

  public TerracottaOperatorEventLogger(NodeIdProvider nodeIdProvider) {
    this.nodeIdProvider = nodeIdProvider;
  }

  public void registerEventCallback(TerracottaOperatorEventCallback callback) {
    this.callbacks.add(callback);
  }

  public void fireOperatorEvent(TerracottaOperatorEvent event) {
    event.setNodeId(this.nodeIdProvider.getNodeId().toString());
    for (TerracottaOperatorEventCallback callback : callbacks) {
      callback.fireOperatorEvent(event);
    }
  }
}
