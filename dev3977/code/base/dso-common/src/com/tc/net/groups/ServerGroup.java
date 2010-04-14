/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.HaConfigImpl;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.NewHaConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.net.GroupID;
import com.tc.object.config.schema.NewL2DSOConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServerGroup {

  private final GroupID     groupId;
  private String[]          members;
  private final NewHaConfig haMode;
  private final Map         nodes;

  public ServerGroup(final ActiveServerGroupConfig group) {
    this.groupId = group.getGroupId();
    this.members = group.getMembers().getMemberArray();
    this.haMode = group.getHa();
    this.nodes = new HashMap();
  }

  public void reloadGroup(L2TVSConfigurationSetupManager manager, final ActiveServerGroupConfig group,
                          List<Node> nodesAdded, List<Node> nodesRemoved) throws ConfigurationSetupException {
    List<String> membersBefore = convertStringToList(this.members);
    List<String> membersNow = convertStringToList(group.getMembers().getMemberArray());

    addNodes(manager, group, nodesAdded, membersNow, membersBefore);
    removeNodes(nodesRemoved, membersNow, membersBefore);

    this.members = group.getMembers().getMemberArray();
  }

  private ArrayList<String> convertStringToList(String[] strArray) {
    ArrayList<String> list = new ArrayList<String>();
    for (String str : strArray) {
      list.add(str);
    }
    return list;
  }

  private void removeNodes(List<Node> nodesRemoved, List<String> membersNow, List<String> membersBefore) {
    membersBefore.removeAll(membersNow);
    for (String member : membersBefore) {
      nodesRemoved.add((Node) this.nodes.remove(member));
    }
  }

  private void addNodes(L2TVSConfigurationSetupManager configSetupManager, ActiveServerGroupConfig group,
                        List<Node> nodesAdded, List<String> membersNow, List<String> membersBefore)
      throws ConfigurationSetupException {
    membersNow.removeAll(membersBefore);
    for (String member : membersNow) {
      NewL2DSOConfig l2 = configSetupManager.dsoL2ConfigFor(member);
      Node node = HaConfigImpl.makeNode(l2);
      nodesAdded.add(node);
    }
  }

  public GroupID getGroupId() {
    return groupId;
  }

  public Collection<Node> getNodes() {
    return getNodes(false);
  }

  public Collection<Node> getNodes(boolean ignoreCheck) {
    Collection c = this.nodes.values();
    if (!ignoreCheck && c.size() != this.members.length) { throw new AssertionError(
                                                                                    "Not all members are present in this collection: collections=["
                                                                                        + getCollectionsToString(c)
                                                                                        + "] members=["
                                                                                        + getMembersToString() + "]"); }
    return c;
  }

  private String getCollectionsToString(Collection c) {
    String out = "";
    for (Iterator iter = c.iterator(); iter.hasNext();) {
      Node node = (Node) iter.next();
      out += node.toString() + " ";
    }
    return out;
  }

  private String getMembersToString() {
    String out = "";
    for (int i = 0; i < this.members.length; i++) {
      out += members[i] + " ";
    }
    return out;
  }

  public void addNode(Node node, String serverName) {
    if (!hasMember(serverName)) { throw new AssertionError("Server=[" + serverName
                                                           + "] is not a member of activeServerGroup=[" + this.groupId
                                                           + "]"); }
    this.nodes.put(serverName, node);
  }

  public Node getNode(String serverName) {
    return (Node) this.nodes.get(serverName);
  }

  public boolean isNetworkedActivePassive() {
    return this.haMode.isNetworkedActivePassive();
  }

  public int getElectionTime() {
    return this.haMode.electionTime();
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerGroup) {
      ServerGroup that = (ServerGroup) obj;
      return this.groupId == that.groupId;
    }
    return false;
  }

  public int hashCode() {
    return groupId.toInt();
  }

  public String toString() {
    return "ActiveServerGroup{groupId=" + groupId + "}";
  }

  public boolean hasMember(String serverName) {
    for (int i = 0; i < this.members.length; i++) {
      if (members[i].equals(serverName)) { return true; }
    }
    return false;
  }

  public List<String> getMembers() {
    return Arrays.asList(members);
  }
}
