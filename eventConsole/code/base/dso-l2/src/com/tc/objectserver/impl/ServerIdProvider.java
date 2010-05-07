/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.NodeIdProvider;
import com.tc.net.NodeID;

public class ServerIdProvider implements NodeIdProvider {
  private final NodeID nodeId;

  public ServerIdProvider(NodeID serverNodeId) {
    this.nodeId = serverNodeId;
  }

  public NodeID getNodeId() {
    return this.nodeId;
  }

}
