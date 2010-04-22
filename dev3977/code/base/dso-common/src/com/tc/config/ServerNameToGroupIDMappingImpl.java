/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.GroupID;
import com.tc.net.groups.Node;

import java.util.HashMap;

public class ServerNameToGroupIDMappingImpl implements ServerNameToGroupIDMapping {
  private volatile HashMap<String, GroupID> serverNameToGidMap = new HashMap<String, GroupID>();

  ServerNameToGroupIDMappingImpl(HashMap<String, GroupID> serverNameToGidMap) {
    this.serverNameToGidMap = serverNameToGidMap;
  }

  public boolean containsServerName(String name) {
    return serverNameToGidMap.containsKey(name);
  }

  public GroupID getGroupIDFromServerName(String name) {
    return serverNameToGidMap.get(name);
  }

  void updateServerNames(ReloadConfigChangeContext context, GroupID gid) {
    HashMap<String, GroupID> tempMap = (HashMap<String, GroupID>) serverNameToGidMap.clone();
    for (Node n : context.getNodesAdded()) {
      tempMap.put(n.getServerNodeName(), gid);
    }

    for (Node n : context.getNodesRemoved()) {
      tempMap.remove(n.getServerNodeName());
    }
    this.serverNameToGidMap = tempMap;
  }
}