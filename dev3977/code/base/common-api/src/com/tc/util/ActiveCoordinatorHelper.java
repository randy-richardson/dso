/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupConfigObject;
import com.tc.net.GroupID;
import com.terracottatech.config.MirrorGroup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.Map.Entry;

public class ActiveCoordinatorHelper {
  private static final String GROUP_NAME_PREFIX = "Tc-Group-";

  private static String getGroupNameFrom(String[] members) {
    String[] temp = new String[members.length];
    for (int i = 0; i < temp.length; i++) {
      temp[i] = members[i];
    }
    Arrays.sort(temp);

    StringBuffer grpName = new StringBuffer();
    for (int i = 0; i < temp.length; i++) {
      grpName.append(temp[i]);
    }
    return grpName.toString();
  }

  public static ActiveServerGroupConfigObject[] setGroupNamesIDAndSort(ActiveServerGroupConfigObject[] asgcos) {
    TreeMap<String, ActiveServerGroupConfigObject> grpNamesToGrp = new TreeMap<String, ActiveServerGroupConfigObject>();

    for (int i = 0; i < asgcos.length; i++) {
      String groupName = null;
      if (groupNameNotSet(asgcos[i])) {
        groupName = getGroupNameFrom(asgcos[i].getMembers().getMemberArray());
      } else {
        groupName = asgcos[i].getGroupName();
      }

      grpNamesToGrp.put(groupName, asgcos[i]);
    }

    ActiveServerGroupConfigObject[] rv = new ActiveServerGroupConfigObject[asgcos.length];
    int counter = 0;
    for (Entry<String, ActiveServerGroupConfigObject> entry : grpNamesToGrp.entrySet()) {
      ActiveServerGroupConfigObject asgco = entry.getValue();
      if (groupNameNotSet(asgco)) {
        asgco.setGroupName(GROUP_NAME_PREFIX + counter);
      }
      asgco.setGroupId(new GroupID(counter));
      rv[counter] = asgco;
      counter++;
    }

    return rv;
  }

  public static MirrorGroup[] setGroupNamesAndSort(MirrorGroup[] mirrorGroups) {
    TreeMap<String, MirrorGroup> grpNamesToGrp = new TreeMap<String, MirrorGroup>();

    for (int i = 0; i < mirrorGroups.length; i++) {
      String groupName = null;
      if (groupNameNotSet(mirrorGroups[i])) {
        groupName = getGroupNameFrom(mirrorGroups[i].getMembers().getMemberArray());
      } else {
        groupName = mirrorGroups[i].getGroupName();
      }

      grpNamesToGrp.put(groupName, mirrorGroups[i]);
    }

    MirrorGroup[] rv = new MirrorGroup[mirrorGroups.length];
    int counter = 0;
    for (Entry<String, MirrorGroup> entry : grpNamesToGrp.entrySet()) {
      MirrorGroup mirrorGrp = entry.getValue();
      if (groupNameNotSet(mirrorGrp)) {
        mirrorGrp.setGroupName(GROUP_NAME_PREFIX + counter);
      }
      rv[counter] = mirrorGrp;
      counter++;
    }

    return rv;
  }

  private static boolean groupNameNotSet(ActiveServerGroupConfigObject asgco) {
    return asgco.getGroupName() == null || asgco.getGroupName() == "";
  }

  private static boolean groupNameNotSet(MirrorGroup asgco) {
    return asgco.getGroupName() == null || asgco.getGroupName() == "";
  }

  public static class ActiveGroupIDComparator implements Comparator<ActiveServerGroupConfig> {
    public int compare(ActiveServerGroupConfig obj1, ActiveServerGroupConfig obj2) {
      return obj1.getGroupId().compareTo(obj2.getGroupId());
    }
  }

  public static class MirrorGroupNameComparator implements Comparator<MirrorGroup> {
    public int compare(MirrorGroup obj1, MirrorGroup obj2) {
      return obj1.getGroupName().compareTo(obj2.getGroupName());
    }
  }
}
