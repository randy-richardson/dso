/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.StandardL2TVSConfigurationSetupManager;
import com.tc.util.ActiveCoordinatorHelper;
import com.terracottatech.config.Ha;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Servers;

import java.util.Comparator;

public class ActiveServerGroupsConfigObject extends BaseNewConfigObject implements ActiveServerGroupsConfig {
  private final ActiveServerGroupConfig[] groupConfigArray;
  private final int                       activeServerGroupCount;

  public ActiveServerGroupsConfigObject(ConfigContext context, StandardL2TVSConfigurationSetupManager setupManager)
      throws ConfigurationSetupException {
    super(context);
    context.ensureRepositoryProvides(MirrorGroups.class);

    MirrorGroups groups = (MirrorGroups) context.bean();
    if (groups == null) { throw new AssertionError(
                                                   "ActiveServerGroups is null!  This should never happen since we make sure default is used."); }

    final MirrorGroup[] groupArray = groups.getMirrorGroupArray();

    if (groupArray == null || groupArray.length == 0) { throw new AssertionError(
                                                                                 "ActiveServerGroup array is null!  This should never happen since we make sure default is used."); }

    this.activeServerGroupCount = groupArray.length;

    ActiveServerGroupConfigObject[] tempGroupConfigArray = new ActiveServerGroupConfigObject[groupArray.length];

    for(int i = 0; i < tempGroupConfigArray.length; i++){
      tempGroupConfigArray[i] = new ActiveServerGroupConfigObject(createContext(setupManager, groupArray[i]), setupManager);
    }
    this.groupConfigArray = ActiveCoordinatorHelper.generateGroupInfo(tempGroupConfigArray);
  }

  public int getActiveServerGroupCount() {
    return this.activeServerGroupCount;
  }

  public ActiveServerGroupConfig[] getActiveServerGroupArray() {
    return groupConfigArray;
  }

  public ActiveServerGroupConfig getActiveServerGroupForL2(String name) {
    for (int groupCount = 0; groupCount < activeServerGroupCount; groupCount++) {
      if (groupConfigArray[groupCount].isMember(name)) { return groupConfigArray[groupCount]; }
    }
    return null;
  }

  public static class ActiveGroupNameComparator implements Comparator<ActiveServerGroupConfig> {
    public int compare(ActiveServerGroupConfig obj1, ActiveServerGroupConfig obj2) {
      return obj1.getGroupName().compareTo(obj2.getGroupName());
    }
  }
  

  private final ConfigContext createContext(StandardL2TVSConfigurationSetupManager setupManager, final MirrorGroup group) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(),
                                                                 MirrorGroup.class, new ChildBeanFetcher() {
                                                                   public XmlObject getChild(XmlObject parent) {
                                                                     return group;
                                                                   }
                                                                 });
    return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
  }

  public static void createDefaultServerMirrorGroups(Servers servers, DefaultValueProvider defaultValueProvider)
      throws ConfigurationSetupException {
    Ha ha;
    try {
      ha = servers.isSetHa() ? servers.getHa() : NewHaConfigObject.getDefaultCommonHa(servers, defaultValueProvider);
    } catch (XmlException e) {
      throw new ConfigurationSetupException(e);
    }
    servers.addNewMirrorGroups();
    ActiveServerGroupConfigObject.createDefaultMirrorGroup(servers, ha);
  }
}
