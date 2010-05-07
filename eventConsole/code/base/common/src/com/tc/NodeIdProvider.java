/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc;

import com.tc.net.NodeID;

public interface NodeIdProvider {
  NodeID getNodeId();
}
