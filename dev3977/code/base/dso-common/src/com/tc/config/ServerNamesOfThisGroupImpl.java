/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.groups.Node;

import java.util.HashSet;
import java.util.Set;

public class ServerNamesOfThisGroupImpl implements ServerNamesOfThisGroup {
  public volatile HashSet<String> serverNames = new HashSet<String>();

  public ServerNamesOfThisGroupImpl(Set<String> set) {
    serverNames.addAll(set);
  }

  public boolean containsServer(String serverName) {
    return serverNames.contains(serverName);
  }

  void updateServerNames(ReloadConfigChangeContext context) {
    HashSet<String> tmp = (HashSet<String>) serverNames.clone();

    for (Node n : context.getNodesAdded()) {
      tmp.add(n.getServerNodeName());
    }

    for (Node n : context.getNodesRemoved()) {
      tmp.remove(n.getServerNodeName());
    }

    this.serverNames = tmp;
  }
}
