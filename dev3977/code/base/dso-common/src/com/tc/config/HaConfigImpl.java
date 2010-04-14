/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HaConfigImpl implements HaConfig {

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final ServerGroup[]                  groups;
  private Node[]                               thisGroupNodes;
  private final Set                            allNodes;
  private final ServerGroup                    activeCoordinatorGroup;
  private final Node                           thisNode;
  private final ServerGroup                    thisGroup;

  /**
   * Need name to group id for StripeTCGroupManagerImpl Map<NodeName, GID> Need node names for this grp
   */
  private HashSet<String>                      nodeNamesForThisGrp = new HashSet<String>();
  private HashMap<String, GroupID>             nodeNameToGidMap    = new HashMap<String, GroupID>();

  public HaConfigImpl(L2TVSConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    ActiveServerGroupsConfig groupsConfig = this.configSetupManager.activeServerGroupsConfig();
    int groupCount = groupsConfig.getActiveServerGroupCount();
    this.groups = new ServerGroup[groupCount];
    for (int i = 0; i < groupCount; i++) {
      this.groups[i] = new ServerGroup(groupsConfig.getActiveServerGroupArray()[i]);
    }
    // Create GroupIds array and get the active-coordinator group id
    GroupID[] grpIds = new GroupID[groupCount];
    for (int i = 0; i < groupCount; i++) {
      grpIds[i] = groups[i].getGroupId();
    }
    GroupID activeCoordinatorGroupId = new OrderedGroupIDs(grpIds).getActiveCoordinatorGroup();
    ServerGroup tempServerGrp = null;
    // Search for the group id which matches activeCoordinator GroupId
    for (int i = 0; i < groupCount; i++) {
      if (activeCoordinatorGroupId.equals(groups[i].getGroupId())) {
        tempServerGrp = groups[i];
        break;
      }
    }

    this.activeCoordinatorGroup = tempServerGrp;
    Assert.assertNotNull(this.activeCoordinatorGroup);

    this.thisGroupNodes = makeThisGroupNodes();
    this.allNodes = makeAllNodes();
    this.thisNode = makeThisNode();
    this.thisGroup = getThisGroupFrom(this.groups, this.configSetupManager.getActiveServerGroupForThisL2());

    buildServerGroupIDMap();
    buildNodeNamesForThisGroup();
  }

  private void buildServerGroupIDMap() {
    for (ServerGroup group : groups) {
      for (Node node : group.getNodes()) {
        nodeNameToGidMap.put(node.getServerNodeName(), group.getGroupId());
      }
    }
  }

  private void buildNodeNamesForThisGroup() {
    for (Node n : thisGroup.getNodes()) {
      nodeNamesForThisGrp.add(n.getServerNodeName());
    }
  }

  /**
   * @return true if nodes are removed
   * @throws ConfigurationSetupException
   */
  public void reloadConfig(List<Node> nodesAdded, List<Node> nodesRemoved) throws ConfigurationSetupException {
    ActiveServerGroupsConfig asgsc = this.configSetupManager.activeServerGroupsConfig();
    int grpCount = asgsc.getActiveServerGroupCount();

    ActiveServerGroupConfig[] asgcArray = asgsc.getActiveServerGroupArray();
    for (int i = 0; i < grpCount; i++) {
      GroupID gid = asgcArray[i].getGroupId();
      for (int j = 0; j < grpCount; j++) {
        if (groups[i].getGroupId().equals(gid)) {
          ArrayList<Node> tempAdded = new ArrayList<Node>();
          ArrayList<Node> tempRemoved = new ArrayList<Node>();
          groups[i].reloadGroup(this.configSetupManager, asgcArray[i], tempAdded, tempRemoved);

          nodesAdded.addAll(tempAdded);
          nodesRemoved.addAll(tempRemoved);

          addAndRemoveNameToGid(tempAdded, tempRemoved, gid);

          if (groups[i] == this.thisGroup) {
            addAndRemoveNamesForThisGrp(tempAdded, tempRemoved);
          }
        }
      }
    }
    
    allNodes.addAll(nodesAdded);
    allNodes.removeAll(nodesRemoved);

    Collection<Node> nodes = this.thisGroup.getNodes(true);
    Node[] tempNodeArray = new Node[nodes.size()];
    nodes.toArray(tempNodeArray);
    this.thisGroupNodes = tempNodeArray;
  }

  private void addAndRemoveNameToGid(ArrayList<Node> tempAdded, ArrayList<Node> tempRemoved, GroupID gid) {
    for (Node n : tempAdded) {
      nodeNameToGidMap.put(n.getServerNodeName(), gid);
    }

    for (Node n : tempRemoved) {
      nodeNameToGidMap.remove(n.getServerNodeName());
    }
  }

  private void addAndRemoveNamesForThisGrp(ArrayList<Node> tempAdded, ArrayList<Node> tempRemoved) {
    for (Node n : tempAdded) {
      nodeNamesForThisGrp.add(n.getServerNodeName());
    }

    for (Node n : tempRemoved) {
      nodeNamesForThisGrp.remove(n.getServerNodeName());
    }
  }

  private ServerGroup getThisGroupFrom(ServerGroup[] sg, ActiveServerGroupConfig activeServerGroupForThisL2) {
    for (int i = 0; i < sg.length; i++) {
      if (sg[i].getGroupId() == activeServerGroupForThisL2.getGroupId()) { return sg[i]; }
    }
    throw new RuntimeException("Unable to find this group information for " + this.thisNode + " "
                               + activeServerGroupForThisL2);
  }

  public boolean isActiveActive() {
    return this.configSetupManager.activeServerGroupsConfig().getActiveServerGroupCount() > 1;
  }

  public boolean isDiskedBasedActivePassive() {
    return !isActiveActive() && !isNetworkedActivePassive();
  }

  public boolean isNetworkedActivePassive() {
    return this.configSetupManager.haConfig().isNetworkedActivePassive();
  }

  public ServerGroup getActiveCoordinatorGroup() {
    return this.activeCoordinatorGroup;
  }

  public ServerGroup[] getAllActiveServerGroups() {
    return this.groups;
  }

  private Node[] makeThisGroupNodes() {
    ActiveServerGroupConfig asgc = this.configSetupManager.getActiveServerGroupForThisL2();
    Assert.assertNotNull(asgc);
    String[] l2Names = asgc.getMembers().getMemberArray();
    Node[] rv = new Node[l2Names.length];

    for (int i = 0; i < l2Names.length; i++) {
      NewL2DSOConfig l2;
      try {
        l2 = this.configSetupManager.dsoL2ConfigFor(l2Names[i]);
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException("Error getting l2 config for: " + l2Names[i], e);
      }
      rv[i] = makeNode(l2);
      addNodeToGroup(rv[i], l2Names[i]);
    }
    return rv;
  }

  public Node[] getThisGroupNodes() {
    return this.thisGroupNodes;
  }

  private Set makeAllNodes() {
    Set allClusterNodes = new HashSet();
    ActiveServerGroupConfig[] asgcs = this.configSetupManager.activeServerGroupsConfig().getActiveServerGroupArray();
    for (int j = 0; j < asgcs.length; ++j) {
      ActiveServerGroupConfig asgc = asgcs[j];
      Assert.assertNotNull(asgc);
      String[] l2Names = asgc.getMembers().getMemberArray();
      for (int i = 0; i < l2Names.length; i++) {
        try {
          NewL2DSOConfig l2 = this.configSetupManager.dsoL2ConfigFor(l2Names[i]);
          Node node = makeNode(l2);
          allClusterNodes.add(node);
          addNodeToGroup(node, l2Names[i]);
        } catch (ConfigurationSetupException e) {
          throw new RuntimeException("Error getting l2 config for: " + l2Names[i], e);
        }
      }
    }
    return allClusterNodes;
  }

  public Node[] getAllNodes() {
    Assert.assertTrue(this.allNodes.size() > 0);
    return (Node[]) this.allNodes.toArray(new Node[this.allNodes.size()]);
  }

  // servers and groups were checked in configSetupManger
  private void addNodeToGroup(Node node, String serverName) {
    boolean added = false;
    for (int i = 0; i < this.groups.length; i++) {
      if (this.groups[i].hasMember(serverName)) {
        this.groups[i].addNode(node, serverName);
        added = true;
      }
    }
    if (!added) { throw new AssertionError("Node=[" + node + "] with serverName=[" + serverName
                                           + "] was not added to any group!"); }
  }

  public Node getThisNode() {
    return this.thisNode;
  }

  public ServerGroup getThisGroup() {
    return this.thisGroup;
  }

  private Node makeThisNode() {
    NewL2DSOConfig l2 = this.configSetupManager.dsoL2Config();
    return makeNode(l2);
  }

  public static Node makeNode(NewL2DSOConfig l2) {
    return new Node(l2.host().getString(), l2.listenPort().getInt(), l2.l2GroupPort().getInt(),
                    TCSocketAddress.WILDCARD_IP);
  }

  public boolean isActiveCoordinatorGroup() {
    return this.thisGroup == this.activeCoordinatorGroup;
  }

  public HashMap<String, GroupID> getNodeNamesToGidMap() {
    return nodeNameToGidMap;
  }

  public HashSet<String> getNodeNames() {
    return nodeNamesForThisGrp;
  }
}
