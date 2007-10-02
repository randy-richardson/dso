/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.ObjectArrayConfigItem;
import com.tc.config.schema.dynamic.ObjectArrayXPathBasedConfigItem;
import com.tc.util.Assert;
import com.terracottatech.config.ActiveServerGroup;
import com.terracottatech.config.ActiveServerGroups;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link L2ConfigForL1}.
 */
public class L2ConfigForL1Object implements L2ConfigForL1 {

  private static final String           DEFAULT_HOST = "localhost";

  private final ConfigContext           l2sContext;
  private final ConfigContext           systemContext;

  private final ObjectArrayConfigItem   l2Data;
  private final L2Data                  defaultL2Data;
  private final ObjectArrayConfigItem[] l2DataByGroup;

  private final Map                     l2DataByName;
  private final Map                     l2DataByGroupId;

  public L2ConfigForL1Object(ConfigContext l2sContext, ConfigContext systemContext) {
    this(l2sContext, systemContext, null);
  }

  public L2ConfigForL1Object(ConfigContext l2sContext, ConfigContext systemContext, int[] dsoPorts) {
    Assert.assertNotNull(l2sContext);
    Assert.assertNotNull(systemContext);

    this.l2sContext = l2sContext;
    this.systemContext = systemContext;

    this.l2sContext.ensureRepositoryProvides(Servers.class);
    this.systemContext.ensureRepositoryProvides(System.class);

    this.l2DataByName = new HashMap();
    this.l2DataByGroupId = new HashMap();

    this.defaultL2Data = new L2Data(DEFAULT_HOST, getL2IntDefault("server/dso-port"));

    this.l2Data = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".", new L2Data[] { defaultL2Data }) {
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        Server[] l2Array = ((Servers) xmlObject).getServerArray();
        L2Data[] data;

        if (l2Array == null || l2Array.length == 0) {
          data = new L2Data[] { defaultL2Data };
        } else {
          data = new L2Data[l2Array.length];

          for (int i = 0; i < data.length; ++i) {
            Server l2 = l2Array[i];
            String host = l2.getHost();
            if (host == null) host = l2.getName();

            if (host == null) host = defaultL2Data.host();
            int dsoPort = l2.getDsoPort() > 0 ? l2.getDsoPort() : defaultL2Data.dsoPort();

            data[i] = new L2Data(host, dsoPort);
            l2DataByName.put(host, data[i]);
          }
        }

        organizeByGroup(xmlObject);

        return data;
      }

      private void organizeByGroup(XmlObject xmlObject) {
        ActiveServerGroups[] asgsArray = ((Servers) xmlObject).getActiveServerGroupsArray();
        Assert.assertEquals(1, asgsArray.length);
        ActiveServerGroup[] asgArray = asgsArray[0].getActiveServerGroupArray();
        Assert.assertNotNull(asgArray);

        for (int i = 0; i < asgArray.length; i++) {
          int groupId = asgArray[i].getId();
          String[] members = asgArray[i].getMembers().getMemberArray();
          List groupList = (List) l2DataByGroupId.get(new Integer(groupId));
          if (groupList == null) {
            groupList = new ArrayList();
            l2DataByGroupId.put(new Integer(groupId), groupList);
          }
          for (int j = 0; j < members.length; j++) {
            L2Data data = (L2Data) l2DataByName.get(members[j]);
            Assert.assertNotNull(data);
            data.setGroupId(groupId);
            groupList.add(data);
          }
        }
      }
    };

    Set keys = this.l2DataByGroupId.keySet();
    this.l2DataByGroup = new ObjectArrayConfigItem[keys.size()];
    int l2DataByGroupPosition = 0;
    for (Iterator iter = keys.iterator(); iter.hasNext();) {
      Integer key = (Integer) iter.next();
      final L2Data[] l2DataArray = (L2Data[]) new ArrayList((List) this.l2DataByGroupId.get(key)).toArray();
      this.l2DataByGroup[l2DataByGroupPosition] = new ObjectArrayXPathBasedConfigItem(this.l2sContext, ".",
          new L2Data[] { defaultL2Data }) {
        protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
          return l2DataArray;
        }
      };
      l2DataByGroupPosition++;
    }

  }

  private int getL2IntDefault(String xpath) {
    try {
      return ((XmlInteger) l2sContext.defaultFor(xpath)).getBigIntegerValue().intValue();
    } catch (XmlException xmle) {
      throw Assert.failure("Can't fetch default for " + xpath + "?", xmle);
    }
  }

  public ObjectArrayConfigItem l2Data() {
    return this.l2Data;
  }

  public ObjectArrayConfigItem[] getL2DataByGroup() {
    return this.l2DataByGroup;
  }

}
