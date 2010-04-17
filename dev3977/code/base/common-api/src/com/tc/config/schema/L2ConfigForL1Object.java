/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerInfo;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayXPathBasedConfigItem;
import com.tc.util.ActiveCoordinatorHelper;
import com.tc.util.Assert;
import com.terracottatech.config.Members;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Object implements L2ConfigForL1 {

  private static final String                 DEFAULT_HOST   = "localhost";

  private final ConfigContext                 l2sContext;
  private final ConfigContext                 systemContext;

  private final ObjectArrayConfigItem         l2Data;
  private final L2Data                        defaultL2Data;
  private final Map                           l2DataByName;
  private final LinkedHashMap                 l2DataByGroupId;
  private ObjectArrayConfigItem[]             l2DataByGroup;
  private boolean                             isActiveActive = false;
  private static HashMap<Set<String>, String> serverNamesToGroupNameFromL2;

  /**
   * To be used only while constructing <code>l2DataByGroup</code><br>
   * Fragile: handle with care ;)
   */
  private MirrorGroup[]                       tempActiveServerGroupArray;

  public L2ConfigForL1Object(final ConfigContext l2sContext, final ConfigContext systemContext) {
    this(l2sContext, systemContext, null);
  }

  public L2ConfigForL1Object(final ConfigContext l2sContext, final ConfigContext systemContext, final int[] dsoPorts) {
    Assert.assertNotNull(l2sContext);
    Assert.assertNotNull(systemContext);

    this.l2sContext = l2sContext;
    this.systemContext = systemContext;

    this.l2sContext.ensureRepositoryProvides(Servers.class);
    this.systemContext.ensureRepositoryProvides(System.class);

    this.l2DataByName = new HashMap();
    this.l2DataByGroupId = new LinkedHashMap();

    this.defaultL2Data = new L2Data(DEFAULT_HOST, getL2IntDefault("server/dso-port"));

    this.l2Data = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".", new L2Data[] { this.defaultL2Data }) {
      @Override
      protected Object fetchDataFromXmlObject(final XmlObject xmlObject) {
        Server[] l2Array = ((Servers) xmlObject).getServerArray();
        L2Data[] data;

        if (l2Array == null || l2Array.length == 0) {
          data = new L2Data[] { L2ConfigForL1Object.this.defaultL2Data };
        } else {
          data = new L2Data[l2Array.length];

          for (int i = 0; i < data.length; ++i) {
            Server l2 = l2Array[i];
            String host = l2.getHost();
            String name = l2.getName();

            if (host == null) {
              host = L2ConfigForL1Object.this.defaultL2Data.host();
            }

            int dsoPort = l2.getDsoPort() > 0 ? l2.getDsoPort() : L2ConfigForL1Object.this.defaultL2Data.dsoPort();

            if (name == null) {
              name = host + ":" + dsoPort;
            }

            data[i] = new L2Data(host, dsoPort);
            L2ConfigForL1Object.this.l2DataByName.put(name, data[i]);
          }
        }

        tempActiveServerGroupArray = readOrConstructActiveServerGroups(xmlObject);
        return data;
      }
    };
  }

  private MirrorGroup[] readOrConstructActiveServerGroups(final XmlObject xmlObject) {
    MirrorGroups asgs = ((Servers) xmlObject).getMirrorGroups();
    if (asgs == null) {
      asgs = ((Servers) xmlObject).addNewMirrorGroups();
    }
    MirrorGroup[] asgArray = asgs.getMirrorGroupArray();
    if (asgArray == null || asgArray.length == 0) {
      MirrorGroup group = asgs.addNewMirrorGroup();
      Members members = group.addNewMembers();
      for (Iterator iter = L2ConfigForL1Object.this.l2DataByName.keySet().iterator(); iter.hasNext();) {
        String host = (String) iter.next();
        members.addMember(host);
      }
      asgArray = asgs.getMirrorGroupArray();
    }
    Assert.assertNotNull(asgArray);
    Assert.assertTrue(asgArray.length >= 1);

    isActiveActive = asgArray.length > 1 ? true : false;
    return asgArray;
  }

  private void setGroups(Map<Set<String>, String> membersToGroupNameMap) {
    // Set group names if not already set
    for (int i = 0; i < tempActiveServerGroupArray.length; i++) {
      String groupName = tempActiveServerGroupArray[i].getGroupName();
      if (groupName == null) {
        HashSet<String> tempMembers = new HashSet<String>();
        Collections.addAll(tempMembers, tempActiveServerGroupArray[i].getMembers().getMemberArray());

        groupName = membersToGroupNameMap.get(tempMembers);
        if (groupName == null && tempActiveServerGroupArray.length == 1) {
          groupName = ActiveCoordinatorHelper.getGroupNameFrom(tempActiveServerGroupArray[i].getMembers()
              .getMemberArray());
        }
        tempActiveServerGroupArray[i].setGroupName(groupName);
      }
    }
    // Sort the array according to the group names
    Arrays.sort(tempActiveServerGroupArray, new MirrorGroupNameComparator());

    for (int i = 0; i < tempActiveServerGroupArray.length; i++) {
      String[] members = tempActiveServerGroupArray[i].getMembers().getMemberArray();
      List groupList = (List) L2ConfigForL1Object.this.l2DataByGroupId.get(new Integer(i));
      if (groupList == null) {
        groupList = new ArrayList();
        L2ConfigForL1Object.this.l2DataByGroupId.put(new Integer(i), groupList);
      }
      for (String member : members) {
        L2Data data = (L2Data) L2ConfigForL1Object.this.l2DataByName.get(member);
        if (data == null) { throw new RuntimeException(
                                                       "The member \""
                                                           + member
                                                           + "\" is not persent in the server section. Please verify the configuration."); }
        Assert.assertNotNull(data);
        data.setGroupId(i);
        String groupName = tempActiveServerGroupArray[i].getGroupName();
        data.setGroupName(groupName);
        groupList.add(data);
      }
    }
  }

  private int getL2IntDefault(final String xpath) {
    try {
      return ((XmlInteger) this.l2sContext.defaultFor(xpath)).getBigIntegerValue().intValue();
    } catch (XmlException xmle) {
      throw Assert.failure("Can't fetch default for " + xpath + "?", xmle);
    }
  }

  public ObjectArrayConfigItem l2Data() {
    return this.l2Data;
  }

  public synchronized ObjectArrayConfigItem[] getL2DataByGroup() {
    if (this.l2DataByGroup == null) {
      setGroups(serverNamesToGroupNameFromL2);
      createL2DataByGroup();
      Assert.assertNotNull(l2DataByGroup);
    }

    Assert.assertNoNullElements(this.l2DataByGroup);
    return this.l2DataByGroup;
  }

  private void createL2DataByGroup() {
    Set keys = this.l2DataByGroupId.keySet();
    Assert.assertTrue(keys.size() > 0);

    this.l2DataByGroup = new ObjectArrayConfigItem[keys.size()];

    int l2DataByGroupPosition = 0;
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      Integer key = (Integer) iter.next();
      List l2DataList = (List) this.l2DataByGroupId.get(key);
      final L2Data[] l2DataArray = new L2Data[l2DataList.size()];
      int position = 0;
      for (Iterator iterator = l2DataList.iterator(); iterator.hasNext();) {
        L2Data data = (L2Data) iterator.next();
        l2DataArray[position++] = data;
      }
      setL2DataInGrp(l2DataByGroupPosition, l2DataArray);
      l2DataByGroupPosition++;
    }
  }

  private void setL2DataInGrp(final int l2DataByGroupPosition, final L2Data[] l2DataArray) {
    this.l2DataByGroup[l2DataByGroupPosition] = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".",
        new L2Data[] { this.defaultL2Data }) {
      @Override
      protected Object fetchDataFromXmlObject(final XmlObject xmlObject) {
        return l2DataArray;
      }
    };
  }

  public static class MirrorGroupNameComparator implements Comparator<MirrorGroup> {
    public int compare(MirrorGroup obj1, MirrorGroup obj2) {
      return obj1.getGroupName().compareTo(obj2.getGroupName());
    }
  }

  public boolean isActiveActive() {
    return isActiveActive;
  }

  public synchronized boolean updateGroupNames(ServerGroups serverGroupsFromL2) {
    serverNamesToGroupNameFromL2 = createMembersToGroupName(serverGroupsFromL2);
    return false;
  }

  private HashMap<Set<String>, String> createMembersToGroupName(ServerGroups serverGroupsFromL2) {
    HashMap<Set<String>, String> map = new HashMap<Set<String>, String>();

    ServerGroup[] serverGrps = serverGroupsFromL2.getServerGroupArray();
    for (ServerGroup serverGrp : serverGrps) {
      String grpName = serverGrp.getGroupName();
      ServerInfo[] serverInfos = serverGrp.getServerInfoArray();
      HashSet<String> serverNames = new HashSet<String>();
      for (ServerInfo serverInfo : serverInfos) {
        serverNames.add(serverInfo.getMemberName());
      }
      map.put(serverNames, grpName);
    }

    return map;
  }
}
