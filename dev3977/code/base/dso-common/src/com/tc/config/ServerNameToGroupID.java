/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.GroupID;
import com.tc.net.groups.Node;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerNameToGroupID {
  private HashMap<String, GroupID> serverNameToGidMap = new HashMap<String, GroupID>();

  ServerNameToGroupID(HashMap<String, GroupID> serverNameToGidMap) {
    this.serverNameToGidMap = serverNameToGidMap;
  }

  public boolean containsGroupName(String name) {
    return serverNameToGidMap.containsKey(name);
  }

  public GroupID getGroupIDFromServerName(String name) {
    return serverNameToGidMap.get(name);
  }

  public void updateServerNames(ArrayList<Node> tempAdded, ArrayList<Node> tempRemoved, GroupID gid) {
    HashMap<String, GroupID> tempMap = (HashMap<String, GroupID>) serverNameToGidMap.clone();
    for (Node n : tempAdded) {
      tempMap.put(n.getServerNodeName(), gid);
    }

    for (Node n : tempRemoved) {
      tempMap.remove(n.getServerNodeName());
    }
    this.serverNameToGidMap = tempMap;
  }
}