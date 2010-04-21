/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net.groups;

import com.tc.config.HaConfig;
import com.tc.config.ServerNameToGroupID;
import com.tc.exception.ImplementMe;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class HaConfigForGroupNameTests implements HaConfig {

  private final CopyOnWriteArraySet<String> set;

  public HaConfigForGroupNameTests(Set<String> tempSet) {
    this.set = new CopyOnWriteArraySet<String>();
    this.set.addAll(tempSet);
  }

  public ServerGroup getActiveCoordinatorGroup() {
    throw new ImplementMe();
  }

  public ServerGroup[] getAllActiveServerGroups() {
    throw new ImplementMe();
  }

  public Node[] getAllNodes() {
    throw new ImplementMe();
  }

  public CopyOnWriteArraySet<String> getNodeNames() {
    return this.set;
  }

  public ServerNameToGroupID getServerNameToGroupID() {
    throw new ImplementMe();
  }

  public ServerGroup getThisGroup() {
    throw new ImplementMe();
  }

  public Node[] getThisGroupNodes() {
    throw new ImplementMe();
  }

  public Node getThisNode() {
    throw new ImplementMe();
  }

  public boolean isActiveActive() {
    throw new ImplementMe();
  }

  public boolean isActiveCoordinatorGroup() {
    throw new ImplementMe();
  }

  public boolean isDiskedBasedActivePassive() {
    throw new ImplementMe();
  }

  public boolean isNetworkedActivePassive() {
    throw new ImplementMe();
  }

  public void reloadConfiguration(List<Node> nodesAdded, List<Node> nodesRemoved) {
    throw new ImplementMe();
  }
}

