/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TopologyVerifier {
  public static enum TopologyReloadStatus {
    TOPOLOGY_CHANGE_ACCEPTABLE, TOPOLOGY_CHANGE_UNACCEPTABLE, TOPOLOGY_UNCHANGED, SPECIFY_MIRROR_GROUPS, SERVER_STILL_ALIVE
  }

  private final Servers                  oldServersBean;
  private final Servers                  newServersBean;
  private final ActiveServerGroupsConfig oldGroupsInfo;

  TopologyVerifier(MutableBeanRepository oldServers, MutableBeanRepository newServers,
                   ActiveServerGroupsConfig oldGroupsInfo) {
    this.oldServersBean = (Servers) oldServers.bean();
    this.newServersBean = (Servers) newServers.bean();
    this.oldGroupsInfo = oldGroupsInfo;
  }

  /**
   * 
   */
  public TopologyReloadStatus checkAndValidateConfig() {
    // first check if all existing servers config is not changed
    // check ports, dgc info, persistent info
    TopologyReloadStatus topologyStatus = checkExistingServerConfigIsSame();
    if (topologyStatus != TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE) { return topologyStatus; }

    // check if group names consist of the same members as the older ones
    return checkGroupInfo();
  }

  private TopologyReloadStatus checkGroupInfo() {
    if (groupSizeEqualsOne()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE; }

    if (!groupNamesSet()) { return TopologyReloadStatus.SPECIFY_MIRROR_GROUPS; }

    if (!groupNamesSame()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

    if (memberMovedToDifferentGroup()) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  private boolean groupSizeEqualsOne() {
    return oldGroupsInfo.getActiveServerGroupCount() == 1
           && (!newServersBean.isSetMirrorGroups() || newServersBean.getMirrorGroups().getMirrorGroupArray().length == 1);
  }

  private boolean groupNamesSet() {
    MirrorGroup[] newGroupsInfo = newServersBean.getMirrorGroups().getMirrorGroupArray();

    // check to see the group names for all new servers are set
    for (MirrorGroup newGroup : newGroupsInfo) {
      if (!newGroup.isSetGroupName()) { return false; }
    }
    return true;
  }

  private boolean memberMovedToDifferentGroup() {
    MirrorGroup[] newGroupsInfo = newServersBean.getMirrorGroups().getMirrorGroupArray();
    for (MirrorGroup newGroupInfo : newGroupsInfo) {
      String groupName = newGroupInfo.getGroupName();
      for (String member : newGroupInfo.getMembers().getMemberArray()) {
        String previousGrpName = getPreviousGroupName(member);
        if (previousGrpName != null && !groupName.equals(previousGrpName)) { return true; }
      }
    }

    return false;
  }

  private String getPreviousGroupName(String member) {
    for (ActiveServerGroupConfig groupInfo : oldGroupsInfo.getActiveServerGroupArray()) {
      if (groupInfo.isMember(member)) { return groupInfo.getGroupName(); }
    }
    return null;
  }

  private boolean groupNamesSame() {
    MirrorGroup[] newGroupsInfo = newServersBean.getMirrorGroups().getMirrorGroupArray();

    Set<String> newGroupNames = new HashSet<String>();
    for (MirrorGroup newGroup : newGroupsInfo) {
      newGroupNames.add(newGroup.getGroupName());
    }

    Set<String> oldGroupNames = new HashSet<String>();
    for (ActiveServerGroupConfig oldGroupInfo : this.oldGroupsInfo.getActiveServerGroupArray()) {
      oldGroupNames.add(oldGroupInfo.getGroupName());
    }

    boolean areGroupNamesSame = oldGroupNames.equals(newGroupNames);
    return areGroupNamesSame;
  }

  private TopologyReloadStatus checkExistingServerConfigIsSame() {
    Server[] oldServerArray = oldServersBean.getServerArray();
    Map<String, Server> oldServersInfo = new HashMap<String, Server>();
    for (Server server : oldServerArray) {
      oldServersInfo.put(server.getName(), server);
    }

    Server[] newServerArray = newServersBean.getServerArray();
    boolean isTopologyChanged = !(newServerArray.length == oldServerArray.length);
    for (Server newServer : newServerArray) {
      Server oldServer = oldServersInfo.get(newServer.getName());
      if (oldServer != null && !checkServer(oldServer, newServer)) { return TopologyReloadStatus.TOPOLOGY_CHANGE_UNACCEPTABLE; }

      if (oldServer == null) {
        isTopologyChanged = true;
      }
    }

    if (!isTopologyChanged) { return TopologyReloadStatus.TOPOLOGY_UNCHANGED; }

    return TopologyReloadStatus.TOPOLOGY_CHANGE_ACCEPTABLE;
  }

  public Set<String> getRemovedMembers() {
    Server[] oldServerArray = oldServersBean.getServerArray();
    HashSet<String> oldServerNames = new HashSet<String>();
    for (Server server : oldServerArray) {
      oldServerNames.add(server.getName());
    }

    Server[] newServerArray = newServersBean.getServerArray();
    for (Server newServer : newServerArray) {
      oldServerNames.remove(newServer.getName());
    }

    return oldServerNames;
  }

  /**
   * check ports, persistence and mode
   */
  private boolean checkServer(Server oldServer, Server newServer) {
    if ((oldServer.getDsoPort() != newServer.getDsoPort()) || (oldServer.getJmxPort() != newServer.getJmxPort())
        || (oldServer.getL2GroupPort() != newServer.getL2GroupPort())) { return false; }

    if (oldServer.isSetDso() && oldServer.getDso().isSetGarbageCollection()) {
      if (!newServer.isSetDso() || !newServer.getDso().isSetGarbageCollection()) { return false; }

      if ((oldServer.getDso().getGarbageCollection().getEnabled() != newServer.getDso().getGarbageCollection()
          .getEnabled())
          || oldServer.getDso().getGarbageCollection().getInterval() != newServer.getDso().getGarbageCollection()
              .getInterval()) { return false; }
    }

    return true;
  }

}
