/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.objectserver.control;

import org.terracotta.test.util.WaitUtil;

import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class ServerMBeanRetriever {
  private final String      host;
  private final int         jmxPort;
  private DSOMBean          dsoMBean;
  private TCServerInfoMBean tcServerInfoMBean;

  public ServerMBeanRetriever(final String host, final int jmxPort) {
    this.host = host;
    this.jmxPort = jmxPort;
  }

  public L2DumperMBean getL2DumperMBean() throws Exception {
    return getL2DumperMBean(host, jmxPort);
  }

  public DSOMBean getDSOMBean() throws Exception {
    if (dsoMBean != null) {
      try {
        dsoMBean.getLiveObjectCount();
        return dsoMBean;
      } catch (Exception e) {
        // the dsoMBean is dead, re-obtain it.
      }
    }
    dsoMBean = getDSOMBean(host, jmxPort);
    return dsoMBean;
  }

  public TCServerInfoMBean getTCServerInfoMBean() throws Exception {
    if (tcServerInfoMBean != null) {
      try {
        tcServerInfoMBean.isActive();
        return tcServerInfoMBean;
      } catch (Exception e) {
        // the dsoMBean is dead, re-obtain it.
      }
    }
    tcServerInfoMBean = getTCServerInfoMBean(host, jmxPort);
    return tcServerInfoMBean;
  }

  public int getJmxPort() {
    return jmxPort;
  }

  public String getHost() {
    return host;
  }

  public static DSOMBean getDSOMBean(final String host, final int jmxPort) throws Exception {
    final AtomicReference<DSOMBean> dsoMBeanRef = new AtomicReference<DSOMBean>();
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        JMXConnector jmxConnector = null;
        try {
          jmxConnector = JMXUtils.getJMXConnector(host, jmxPort);
          MBeanServerConnection msc = jmxConnector.getMBeanServerConnection();
          DSOMBean dsoMBean = MBeanServerInvocationProxy.newMBeanProxy(msc, L2MBeanNames.DSO, DSOMBean.class, false);
          dsoMBean.getLiveObjectCount();
          dsoMBeanRef.set(dsoMBean);
          return true;
        } catch (Exception e) {
          if (jmxConnector != null) {
            jmxConnector.close();
          }
          return false;
        }
      }
    });
    return dsoMBeanRef.get();
  }

  public static L2DumperMBean getL2DumperMBean(final String host, final int jmxPort) throws Exception {
    // Not going to cache this and check liveness because it's not used super frequently and it doesn't have an easy
    // non-destructive check method.
    final AtomicReference<L2DumperMBean> l2DumperRef = new AtomicReference<L2DumperMBean>();
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        JMXConnector jmxConnector = null;
        try {
          jmxConnector = JMXUtils.getJMXConnector(host, jmxPort);
          MBeanServerConnection msc = jmxConnector.getMBeanServerConnection();
          l2DumperRef.set(MBeanServerInvocationProxy
              .newMBeanProxy(msc, L2MBeanNames.DUMPER, L2DumperMBean.class, false));
          return true;
        } catch (Exception e) {
          if (jmxConnector != null) {
            jmxConnector.close();
          }
          return false;
        }
      }
    });
    return l2DumperRef.get();
  }

  public static TCServerInfoMBean getTCServerInfoMBean(final String host, final int jmxPort) throws Exception {
    final AtomicReference<TCServerInfoMBean> tcServerInfoMBeanRef = new AtomicReference<TCServerInfoMBean>();
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        JMXConnector jmxConnector = null;
        try {
          jmxConnector = JMXUtils.getJMXConnector(host, jmxPort);
          MBeanServerConnection msc = jmxConnector.getMBeanServerConnection();
          tcServerInfoMBeanRef.set(MBeanServerInvocationProxy.newMBeanProxy(msc, L2MBeanNames.TC_SERVER_INFO,
              TCServerInfoMBean.class, false));
          return true;
        } catch (Exception e) {
          if (jmxConnector != null) {
            jmxConnector.close();
          }
          return false;
        }
      }
    });
    return tcServerInfoMBeanRef.get();
  }
}
