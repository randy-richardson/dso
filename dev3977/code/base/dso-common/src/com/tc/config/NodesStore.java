/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.groups.Node;

public interface NodesStore extends ServerNamesOfThisGroup {

  void addListener(TopologyChangeListener listener);

  Node[] getAllNodes();
}