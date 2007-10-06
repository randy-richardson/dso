/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.management.JMXConnectorProxy;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.objectserver.control.ExtraProcessServerControl;
import com.tc.objectserver.control.ServerControl;
import com.tc.util.PortChooser;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.TestState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class MultipleServerManager {
  private static final String                  HOST                   = "localhost";
  private static final String                  SERVER_NAME            = "testserver";
  private static final String                  CONFIG_FILE_NAME       = "active-passive-server-config.xml";
  private static final boolean                 DEBUG                  = false;
  private static final int                     NULL_VAL               = -1;

  private final File                           tempDir;
  private final PortChooser                    portChooser;
  private final String                         configModel;
  private final MultipleServerTestSetupManager setupManger;

  private final int                            serverCount;
  private final String                         serverCrashMode;
  private final long                           serverCrashWaitTimeInSec;
  private final String                         serverPersistence;
  private final boolean                        serverNetworkShare;
  private final ServerConfigCreator            serverConfigCreator;
  private final String                         configFileLocation;
  private final File                           configFile;

  private final ServerInfo[]                   servers;
  private final int[]                          dsoPorts;
  private final int[]                          jmxPorts;
  private final int[]                          l2GroupPorts;
  private final String[]                       serverNames;
  private final TCServerInfoMBean[]            tcServerInfoMBeans;
  private final JMXConnector[]                 jmxConnectors;

  private final List                           errors;

  private final int[]                          activeIndices;
  private int                                  activeIndicesNextIndex = 0;
  private int                                  lastCrashedIndex       = NULL_VAL;
  private ServerCrasher                        serverCrasher;
  private int                                  maxCrashCount;
  private final TestState                      testState;
  private Random                               random;
  private long                                 seed;
  private final File                           javaHome;
  private int                                  pid                    = -1;
  private List                                 jvmArgs                = null;
  private final List[]                         groups;
  private final String                         testMode;

  public MultipleServerManager(File tempDir, PortChooser portChooser, String configModel,
                               MultipleServerTestSetupManager setupManger, File javaHome,
                               TestTVSConfigurationSetupManagerFactory configFactory, List extraJvmArgs, String testMode)
      throws Exception {

    this.jvmArgs = extraJvmArgs;

    this.setupManger = setupManger;
    this.testMode = testMode;
    this.activeIndices = new int[this.setupManger.getActiveServerGroupCount()];
    resetActiveIndices();

    serverCount = this.setupManger.getServerCount();

    if (serverCount < 2) { throw new AssertionError(
                                                    "Multiple server tests involve 2 or more DSO servers: serverCount=["
                                                        + serverCount + "]"); }

    this.tempDir = tempDir;
    configFileLocation = this.tempDir + File.separator + CONFIG_FILE_NAME;
    configFile = new File(configFileLocation);

    this.portChooser = portChooser;
    this.configModel = configModel;

    serverCrashMode = this.setupManger.getServerCrashMode();
    serverCrashWaitTimeInSec = this.setupManger.getServerCrashWaitTimeInSec();
    maxCrashCount = this.setupManger.getMaxCrashCount();
    serverPersistence = this.setupManger.getServerPersistenceMode();
    serverNetworkShare = this.setupManger.isNetworkShare();

    servers = new ServerInfo[this.serverCount];
    dsoPorts = new int[this.serverCount];
    jmxPorts = new int[this.serverCount];
    l2GroupPorts = new int[this.serverCount];
    serverNames = new String[this.serverCount];
    tcServerInfoMBeans = new TCServerInfoMBean[this.serverCount];
    jmxConnectors = new JMXConnector[this.serverCount];
    groups = new List[this.setupManger.getActiveServerGroupCount()];
    createServers();

    serverConfigCreator = new ServerConfigCreator(this.setupManger, dsoPorts, jmxPorts, l2GroupPorts, serverNames,
                                                  groups, this.configModel, configFile, this.tempDir, configFactory,
                                                  this.testMode);
    serverConfigCreator.writeL2Config();

    errors = new ArrayList();
    testState = new TestState();
    this.javaHome = javaHome;

    if (serverCrashMode.equals(ServerCrashMode.RANDOM_SERVER_CRASH)) {
      SecureRandom srandom = SecureRandom.getInstance("SHA1PRNG");
      seed = srandom.nextLong();
      random = new Random(seed);
      System.out.println("***** Random number generator seed=[" + seed + "]");
    }
  }

  private void resetActiveIndices() {
    for (int i = 0; i < this.activeIndices.length; i++) {
      activeIndices[i] = NULL_VAL;
    }
    this.activeIndicesNextIndex = 0;
  }

  private void resetLastCrashedIndex() {
    lastCrashedIndex = NULL_VAL;
  }

  private void createServers() throws FileNotFoundException {
    int startIndex = 0;

    if (DEBUG) {
      dsoPorts[0] = 8510;
      jmxPorts[0] = 8520;
      l2GroupPorts[0] = 8530;
      serverNames[0] = SERVER_NAME + 0;
      servers[0] = new ServerInfo(HOST, serverNames[0], dsoPorts[0], jmxPorts[0], l2GroupPorts[0],
                                  getServerControl(dsoPorts[0], jmxPorts[0], serverNames[0]));
      dsoPorts[1] = 7510;
      jmxPorts[1] = 7520;
      l2GroupPorts[1] = 7530;
      serverNames[1] = SERVER_NAME + 1;
      servers[1] = new ServerInfo(HOST, serverNames[1], dsoPorts[1], jmxPorts[1], l2GroupPorts[1],
                                  getServerControl(dsoPorts[1], jmxPorts[1], serverNames[1]));
      if (dsoPorts.length > 2) {
        dsoPorts[2] = 6510;
        jmxPorts[2] = 6520;
        l2GroupPorts[2] = 6530;
        serverNames[2] = SERVER_NAME + 2;
        servers[2] = new ServerInfo(HOST, serverNames[2], dsoPorts[2], jmxPorts[2], l2GroupPorts[2],
                                    getServerControl(dsoPorts[2], jmxPorts[2], serverNames[2]));
      }

      startIndex = 3;
    }

    for (int i = startIndex; i < dsoPorts.length; i++) {
      setPorts(i);
      serverNames[i] = SERVER_NAME + i;
      servers[i] = new ServerInfo(HOST, serverNames[i], dsoPorts[i], jmxPorts[i], l2GroupPorts[i],
                                  getServerControl(dsoPorts[i], jmxPorts[i], serverNames[i]));
    }

    int position = 0;
    for (int i = 0; i < groups.length; i++) {
      groups[i] = new ArrayList();
      for (int j = 0; j < this.setupManger.getGroupMemberCount(i); j++) {
        groups[i].add(this.serverNames[position++]);
      }
    }
  }

  private void setPorts(int index) {
    while (true) {
      int newPort = portChooser.chooseRandomPort();
      if (isUnusedPort(newPort)) {
        jmxPorts[index] = newPort;
        break;
      }
    }
    while (true) {
      int newPort = portChooser.chooseRandomPort();
      if (newPort == PortChooser.MAX) {
        continue;
      }
      if (portChooser.isPortUsed(newPort + 1)) {
        continue;
      }
      if (isUnusedPort(newPort) && isUnusedPort(newPort + 1)) {
        dsoPorts[index] = newPort;
        l2GroupPorts[index] = newPort + 1;
        break;
      }
    }
  }

  private boolean isUnusedPort(int port) {
    boolean unused = true;
    for (int i = 0; i < dsoPorts.length; i++) {
      if (dsoPorts[i] == port) {
        unused = false;
      }
    }
    for (int i = 0; i < jmxPorts.length; i++) {
      if (jmxPorts[i] == port) {
        unused = false;
      }
    }
    for (int i = 0; i < l2GroupPorts.length; i++) {
      if (l2GroupPorts[i] == port) {
        unused = false;
      }
    }
    return unused;
  }

  private ServerControl getServerControl(int dsoPort, int jmxPort, String serverName) throws FileNotFoundException {
    return new ExtraProcessServerControl(HOST, dsoPort, jmxPort, configFileLocation, true, serverName, this.jvmArgs,
                                         javaHome, true);
  }

  public void startServers() throws Exception {
    verifyActiveIndicesUnset();

    for (int i = 0; i < servers.length; i++) {
      servers[i].getServerControl().start();
    }
    Thread.sleep(500 * servers.length);

    debugPrintln("***** startServers():  about to search for active  threadId=[" + Thread.currentThread().getName()
                 + "]");

    for (int i = 0; i < tcServerInfoMBeans.length; i++) {
      debugPrintln("***** Caching tcServerInfoMBean for server=[" + dsoPorts[i] + "]");
      tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
    }

    getActiveIndices();

    if (serverCrashMode.equals(ServerCrashMode.CONTINUOUS_ACTIVE_CRASH)
        || serverCrashMode.equals(ServerCrashMode.RANDOM_SERVER_CRASH)) {
      startContinuousCrash();
    }
  }

  private void verifyActiveIndicesUnset() {
    for (int i = 0; i < activeIndices.length; i++) {
      if (activeIndices[i] != NULL_VAL) { throw new AssertionError(
                                                                   "Server(s) has/have been already started: activeIndices=["
                                                                       + activeIndicesToString() + "]"); }
    }
  }

  private void startContinuousCrash() {
    serverCrasher = new ServerCrasher(this, serverCrashWaitTimeInSec, maxCrashCount, testState);
    new Thread(serverCrasher).start();
  }

  public void storeErrors(Exception e) {
    if (e != null) {
      synchronized (errors) {
        errors.add(e);
      }
    }
  }

  public List getErrors() {
    synchronized (errors) {
      List l = new ArrayList();
      l.addAll(errors);
      return l;
    }
  }

  private void getActiveIndices() throws Exception {
    while (activeIndicesNextIndex < activeIndices.length) {
      System.out.println("Searching for active server(s)... ");
      for (int i = 0; i < jmxPorts.length; i++) {
        if (i != lastCrashedIndex) {
          if (!servers[i].getServerControl().isRunning()) { throw new AssertionError("Server["
                                                                                     + servers[i].getDsoPort()
                                                                                     + "] is not running as expected!"); }
          boolean isActive;
          try {
            isActive = tcServerInfoMBeans[i].isActive();
          } catch (Exception e) {
            System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[i] + "]...");
            tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
            isActive = tcServerInfoMBeans[i].isActive();
          }

          if (isActive) {
            if (activeIndices.length <= activeIndicesNextIndex) { throw new Exception("More than ["
                                                                                      + activeIndices.length
                                                                                      + "] active servers found."); }

            debugPrintln("***** active found index=[" + i + "] activeIndicesNextIndex=[" + activeIndicesNextIndex + "]");
            activeIndices[activeIndicesNextIndex++] = i;
          }
        }
      }
      Thread.sleep(1000);
    }
  }

  private void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  private void waitForPassive() throws Exception {
    while (true) {
      System.out.println("Searching for appropriate passive server(s)... ");
      for (int i = 0; i < jmxPorts.length; i++) {
        if (!activeIndicesContains(i)) {
          if (!servers[i].getServerControl().isRunning()) { throw new AssertionError("Server["
                                                                                     + servers[i].getDsoPort()
                                                                                     + "] is not running as expected!"); }
          boolean isPassiveStandby;
          try {
            isPassiveStandby = tcServerInfoMBeans[i].isPassiveStandby();
          } catch (Exception e) {
            System.out.println("Need to fetch tcServerInfoMBean for server=[" + dsoPorts[i] + "]...");
            tcServerInfoMBeans[i] = getTcServerInfoMBean(i);
            isPassiveStandby = tcServerInfoMBeans[i].isPassiveStandby();
          }
          if (serverNetworkShare && isPassiveStandby) {
            return;
          } else if (!serverNetworkShare && tcServerInfoMBeans[i].isStarted()) {
            return;
          } else if (tcServerInfoMBeans[i].isActive()) { throw new AssertionError(
                                                                                  "Server["
                                                                                      + servers[i].getDsoPort()
                                                                                      + "] is in active mode when it should not be!"); }
        }
      }
      Thread.sleep(1000);
    }
  }

  private boolean activeIndicesContains(int i) {
    for (int j = 0; j < activeIndices.length; j++) {
      if (activeIndices[j] == i) { return true; }
    }
    return false;
  }

  private TCServerInfoMBean getTcServerInfoMBean(int index) throws IOException {
    if (jmxConnectors[index] != null) {
      closeJMXConnector(index);
    }
    jmxConnectors[index] = getJMXConnector(jmxPorts[index]);
    MBeanServerConnection mBeanServer = jmxConnectors[index].getMBeanServerConnection();
    return (TCServerInfoMBean) MBeanServerInvocationHandler.newProxyInstance(mBeanServer, L2MBeanNames.TC_SERVER_INFO,
                                                                             TCServerInfoMBean.class, true);
  }

  public static JMXConnector getJMXConnector(int jmxPort) throws IOException {
    JMXConnector jmxConnector = new JMXConnectorProxy(HOST, jmxPort);
    jmxConnector.connect();
    return jmxConnector;
  }

  public void stopAllServers() throws Exception {
    synchronized (testState) {
      debugPrintln("***** setting TestState to STOPPING");
      testState.setTestState(TestState.STOPPING);

      closeJMXConnectors();

      for (int i = 0; i < serverCount; i++) {
        debugPrintln("***** stopping server=[" + servers[i].getDsoPort() + "]");
        ServerControl sc = servers[i].getServerControl();

        if (!sc.isRunning()) {
          if (i == lastCrashedIndex) {
            continue;
          } else {
            throw new AssertionError("Server[" + servers[i].getDsoPort() + "] is not running as expected!");
          }
        }

        if (activeIndicesContains(i)) {
          sc.shutdown();
          continue;
        }

        try {
          sc.crash();
        } catch (Exception e) {
          if (DEBUG) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public void dumpAllServers(int currentPid, int dumpCount, long dumpInterval) throws Exception {
    pid = currentPid;
    for (int i = 0; i < serverCount; i++) {
      if (!serverNetworkShare && !activeIndicesContains(i)) {
        debugPrintln("***** skipping dumping server=[" + dsoPorts[i] + "]");
        continue;
      }
      if (servers[i].getServerControl().isRunning()) {
        System.out.println("Dumping server=[" + dsoPorts[i] + "]");

        MBeanServerConnection mbs;
        try {
          mbs = jmxConnectors[i].getMBeanServerConnection();
        } catch (IOException ioe) {
          System.out.println("Need to recreate jmxConnector for server=[" + dsoPorts[i] + "]...");
          jmxConnectors[i] = getJMXConnector(jmxPorts[i]);
          mbs = jmxConnectors[i].getMBeanServerConnection();
        }
        L2DumperMBean mbean = (L2DumperMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DUMPER,
                                                                                            L2DumperMBean.class, true);
        mbean.doServerDump();
        if (pid != 0) {
          mbean.setThreadDumpCount(dumpCount);
          mbean.setThreadDumpInterval(dumpInterval);
          System.out.println("Thread dumping server=[" + dsoPorts[i] + "] pid=[" + pid + "]");
          pid = mbean.doThreadDump();
        }
      }
    }
    closeJMXConnectors();
  }

  private void closeJMXConnector(int i) {
    if (jmxConnectors[i] != null) {
      try {
        jmxConnectors[i].close();
      } catch (Exception e) {
        System.out.println("JMXConnector for server=[" + dsoPorts[i] + "] already closed.");
        e.printStackTrace();
      }
      jmxConnectors[i] = null;
    }
  }

  private void closeJMXConnectors() {
    for (int i = 0; i < jmxConnectors.length; i++) {
      closeJMXConnector(i);
      ThreadUtil.reallySleep(100);
    }
  }

  public int getPid() {
    return pid;
  }

  private String activeIndicesToString() {
    StringBuffer buffer = new StringBuffer();
    boolean first = true;
    for (int i = 0; i < activeIndices.length; i++) {
      if (first) {
        first = false;
      } else {
        buffer.append(",");
      }
      buffer.append("" + activeIndices[i]);
    }
    return buffer.toString();
  }

  // TODO: make this work with multiple servers
  public void crashActive() throws Exception {
    int activeIndex = 0;
    crashActive(activeIndices[activeIndex]);
  }

  public void crashActive(int crashIndex) throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping crash active");
      return;
    }

    if (activeIndicesNextIndex == 0) { throw new AssertionError("Active index was not set."); }

    System.out.println("Crashing active server: dsoPort=[" + servers[crashIndex].getDsoPort() + "]");

    debugPrintln("***** wait to find an appropriate passive server.");
    waitForPassive();
    debugPrintln("***** finished waiting to find an appropriate passive server.");

    verifyActiveServerState(crashIndex);

    ServerControl server = servers[crashIndex].getServerControl();
    server.crash();
    debugPrintln("***** Sleeping after crashing active server ");
    waitForServerCrash(server);
    debugPrintln("***** Done sleeping after crashing active server ");

    lastCrashedIndex = crashIndex;
    resetActiveIndices();
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");

    debugPrintln("***** about to search for active  threadId=[" + Thread.currentThread().getName() + "]");

    getActiveIndices();
    debugPrintln("***** activeIndices[" + activeIndicesToString() + "] ");
  }

  private void verifyActiveServerState(int crashIndex) throws Exception {
    ServerControl server = servers[crashIndex].getServerControl();
    if (!server.isRunning()) { throw new AssertionError("Server[" + servers[crashIndex].getDsoPort()
                                                        + "] is not running as expected!"); }
    MBeanServerConnection mbs = jmxConnectors[crashIndex].getMBeanServerConnection();
    TCServerInfoMBean mbean = (TCServerInfoMBean) MBeanServerInvocationHandler
        .newProxyInstance(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, true);
    if (!mbean.isActive()) {
      closeJMXConnector(crashIndex);
      throw new AssertionError("Server[" + servers[crashIndex].getDsoPort() + "] is not an active server as expected!");
    }
    closeJMXConnector(crashIndex);
  }

  private void waitForServerCrash(ServerControl server) throws Exception {
    long duration = 10000;
    long startTime = System.currentTimeMillis();
    while (duration > (System.currentTimeMillis() - startTime)) {
      if (server.isRunning()) {
        try {
          Thread.sleep(1000);
        } catch (Exception e) {/**/
        }
      } else {
        return;
      }
    }
    throw new Exception("Server crash did not complete.");
  }

  private void crashPassive(int passiveToCrash) throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping crash passive");
      return;
    }

    System.out.println("Crashing passive server: dsoPort=[" + servers[passiveToCrash].getDsoPort() + "]");

    debugPrintln("***** Closing passive's jmxConnector ");
    closeJMXConnector(passiveToCrash);

    ServerControl server = servers[passiveToCrash].getServerControl();
    if (!server.isRunning()) { throw new AssertionError("Server[" + servers[passiveToCrash].getDsoPort()
                                                        + "] is not running as expected!"); }
    server.crash();
    debugPrintln("***** Sleeping after crashing passive server ");
    waitForServerCrash(server);
    debugPrintln("***** Done sleeping after crashing passive server ");

    lastCrashedIndex = passiveToCrash;
    debugPrintln("***** lastCrashedIndex[" + lastCrashedIndex + "] ");
  }

  private void crashRandomServer() throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping crash random server");
      return;
    }

    if (activeIndicesNextIndex == 0) { throw new AssertionError("Active index was not set."); }
    if (random == null) { throw new AssertionError("Random number generator was not set."); }

    debugPrintln("***** Choosing random server... ");

    int crashIndex = random.nextInt(serverCount);

    if (activeIndicesContains(crashIndex)) {
      crashActive(crashIndex);
    } else {
      crashPassive(crashIndex);
    }
  }

  public void restartLastCrashedServer() throws Exception {
    if (!testState.isRunning()) {
      debugPrintln("***** test state is not running ... skipping restart");
      return;
    }

    debugPrintln("*****  restarting crashed server");

    if (lastCrashedIndex >= 0) {
      if (servers[lastCrashedIndex].getServerControl().isRunning()) { throw new AssertionError(
                                                                                               "Server["
                                                                                                   + servers[lastCrashedIndex]
                                                                                                       .getDsoPort()
                                                                                                   + "] is not down as expected!"); }
      servers[lastCrashedIndex].getServerControl().start();

      if (!servers[lastCrashedIndex].getServerControl().isRunning()) { throw new AssertionError(
                                                                                                "Server["
                                                                                                    + servers[lastCrashedIndex]
                                                                                                        .getDsoPort()
                                                                                                    + "] is not running as expected!"); }
      resetLastCrashedIndex();
    } else {
      throw new AssertionError("No crashed servers to restart.");
    }
  }

  public int getServerCount() {
    return serverCount;
  }

  public int[] getDsoPorts() {
    return dsoPorts;
  }

  public int[] getJmxPorts() {
    return jmxPorts;
  }

  public boolean crashActiveServerAfterMutate() {
    if (serverCrashMode.equals(ServerCrashMode.CRASH_AFTER_MUTATE)) { return true; }
    return false;
  }

  public void addServersToL1Config(TestTVSConfigurationSetupManagerFactory configFactory) {
    for (int i = 0; i < serverCount; i++) {

      debugPrintln("******* adding to L1 config: serverName=[" + serverNames[i] + "] dsoPort=[" + dsoPorts[i]
                   + "] jmxPort=[" + jmxPorts[i] + "]");

      configFactory.addServerToL1Config(serverNames[i], dsoPorts[i], jmxPorts[i]);
    }
    for (int i = 0; i < this.groups.length; i++) {
      configFactory.addServerGroupToL1Config(i, this.groups[i]);
    }
  }

  public void crashServer() throws Exception {
    if (serverCrashMode.equals(ServerCrashMode.CONTINUOUS_ACTIVE_CRASH)) {
      crashActive();
    } else if (serverCrashMode.equals(ServerCrashMode.RANDOM_SERVER_CRASH)) {
      crashRandomServer();
    }

    if (serverNetworkShare && serverPersistence.equals(ServerPersistenceMode.PERMANENT_STORE)) {
      System.out.println("Deleting data directory for server=[" + servers[lastCrashedIndex].getDsoPort() + "]");
      deleteDirectory(serverConfigCreator.getDataLocation(lastCrashedIndex));
    }
  }

  private void deleteDirectory(String directory) {
    debugPrintln("\n ##### about to delete dataFile=[" + directory + "] and all of its content...");
    File[] files = new File(directory).listFiles();
    for (int i = 0; i < files.length; i++) {
      if (files[i].isDirectory()) {
        deleteDirectory(files[i].getAbsolutePath());
      } else {
        boolean successful = files[i].delete();
        if (!successful) { throw new AssertionError("delete file=[" + files[i].getAbsolutePath() + "] failed"); }
        debugPrintln("\n ##### deleted file=[" + files[i].getAbsolutePath() + "]");
      }
    }
    if (!(new File(directory).delete())) { throw new AssertionError("delete file=[" + directory + "] failed"); }
    debugPrintln("\n ##### deleted directory=[" + directory + "]");
    debugPrintln("\n ##### dataFile=[" + directory + "] still exists? [" + (new File(directory).exists()) + "]");
  }

  /*
   * Server inner class
   */
  private static class ServerInfo {
    private final String        server_host;
    private final String        server_name;
    private final int           server_dsoPort;
    private final int           server_jmxPort;
    private final int           server_l2GroupPort;
    private final ServerControl serverControl;
    private String              dataLocation;
    private String              logLocation;

    ServerInfo(String host, String name, int dsoPort, int jmxPort, int l2GroupPort, ServerControl serverControl) {
      server_host = host;
      server_name = name;
      server_dsoPort = dsoPort;
      server_jmxPort = jmxPort;
      server_l2GroupPort = l2GroupPort;
      this.serverControl = serverControl;
    }

    public String getHost() {
      return server_host;
    }

    public String getName() {
      return server_name;
    }

    public int getDsoPort() {
      return server_dsoPort;
    }

    public int getJmxPort() {
      return server_jmxPort;
    }

    public int getL2GroupPort() {
      return server_l2GroupPort;
    }

    public ServerControl getServerControl() {
      return serverControl;
    }

    public void setDataLocation(String location) {
      dataLocation = location;
    }

    public String getDataLocation() {
      return dataLocation;
    }

    public void setLogLocation(String location) {
      logLocation = location;
    }

    public String getLogLocation() {
      return logLocation;
    }
  }

}
