/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.net.NodeIdProvider;
import com.tc.object.net.DSOClientMessageChannel;

public class ClientIdProvider implements NodeIdProvider {
  
  private final DSOClientMessageChannel channel;

  public ClientIdProvider(DSOClientMessageChannel channel) {
    this.channel = channel;
  }

  public NodeID getNodeId() {
    return this.channel.channel().getLocalNodeID();
  }

}
