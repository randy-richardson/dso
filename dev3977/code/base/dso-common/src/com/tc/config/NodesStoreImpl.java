/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.groups.Node;
import com.tc.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NodesStoreImpl implements NodesStore {
  private final Set<Node>                                   nodes;
  public volatile HashSet<String>                           serverNamesForThisGroup = new HashSet<String>();
  private final CopyOnWriteArraySet<TopologyChangeListener> listeners               = new CopyOnWriteArraySet<TopologyChangeListener>();

  /**
   * used for tests
   */
  public NodesStoreImpl(Set<Node> nodes) {
    this.nodes = Collections.synchronizedSet(nodes);
  }

  public NodesStoreImpl(Set<Node> nodes, Set<String> nodeNamesForThisGroup) {
    this(nodes);
    serverNamesForThisGroup.addAll(nodeNamesForThisGroup);
  }

  void updateNodes(ReloadConfigChangeContext context) {
    this.nodes.addAll(context.getNodesAdded());
    this.nodes.removeAll(context.getNodesRemoved());

    for (TopologyChangeListener listener : listeners) {
      listener.topologyChanged(context);
    }
  }

  public void addListener(TopologyChangeListener listener) {
    listeners.add(listener);
  }

  public Node[] getAllNodes() {
    Assert.assertTrue(this.nodes.size() > 0);
    return this.nodes.toArray(new Node[this.nodes.size()]);
  }

  public boolean containsServer(String serverName) {
    return serverNamesForThisGroup.contains(serverName);
  }

  void updateServerNames(ReloadConfigChangeContext context) {
    HashSet<String> tmp = (HashSet<String>) serverNamesForThisGroup.clone();

    for (Node n : context.getNodesAdded()) {
      tmp.add(n.getServerNodeName());
    }

    for (Node n : context.getNodesRemoved()) {
      tmp.remove(n.getServerNodeName());
    }

    this.serverNamesForThisGroup = tmp;
  }
}
