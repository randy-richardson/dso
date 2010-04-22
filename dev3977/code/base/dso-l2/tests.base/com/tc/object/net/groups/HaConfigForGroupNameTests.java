/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net.groups;

import com.tc.config.HaConfig;
import com.tc.config.ReloadConfigChangeContext;
import com.tc.config.ServerNameToGroupIDMapping;
import com.tc.config.ServerNamesOfThisGroup;
import com.tc.config.ServerNamesOfThisGroupImpl;
import com.tc.exception.ImplementMe;
import com.tc.net.GroupID;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;

import java.util.Set;

public class HaConfigForGroupNameTests implements HaConfig {

  private final ServerNamesOfThisGroupImpl set;

  public HaConfigForGroupNameTests(Set<String> tempSet) {
    this.set = new ServerNamesOfThisGroupImpl(tempSet);
  }

  public GroupID getActiveCoordinatorGroupID() {
    throw new ImplementMe();
  }

  public ServerGroup[] getAllActiveServerGroups() {
    throw new ImplementMe();
  }

  public Node[] getAllNodes() {
    throw new ImplementMe();
  }
  
  public GroupID getThisGroupID() {
    throw new ImplementMe();
  }

  public GroupID[] getGroupIDs() {
    throw new ImplementMe();
  }
  
  public ServerNamesOfThisGroup getServerNamesOfThisGroup() {
    return this.set;
  }

  public ServerNameToGroupIDMapping getServerNameToGroupID() {
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

  public ReloadConfigChangeContext reloadConfiguration() {
    throw new ImplementMe();
  }
}

