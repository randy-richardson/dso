/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.FromSchemaDefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory.ConfigMode;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.Ha;
import com.terracottatech.config.HaMode;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class BaseTVSConfigurationSetupManagerTest extends TCTestCase {

  private static final String DEFAULT_CONFIG_SPEC       = "tc-config.xml";
  private static final String CONFIG_SPEC_ARGUMENT_NAME = "config";
  private static final String CONFIG_FILE_PROPERTY_NAME = "tc.config";
  private static final String DEFAULT_CONFIG_PATH       = "default-config.xml";
  private static final String DEFAULT_CONFIG_URI        = "resource:///"
                                                          + BaseTVSConfigurationSetupManagerTest.class.getPackage()
                                                              .getName().replace('.', '/') + "/" + DEFAULT_CONFIG_PATH;
  private File                tcConfig                  = null;

  public void testServerDefaults1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    Assert.assertEquals(9510, server.getDsoPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getDsoPort().getBind());

    int tempGroupPort = 9510 + BaseTVSConfigurationSetupManager.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    int tempJmxPort = 9510 + BaseTVSConfigurationSetupManager.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getL2GroupPort().getBind());

  }

  public void testServerDefaults2() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<dso-port>8513</dso-port>" + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getDsoPort().getBind());

    int tempGroupPort = dsoPort + BaseTVSConfigurationSetupManager.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    int tempJmxPort = dsoPort + BaseTVSConfigurationSetupManager.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getL2GroupPort().getBind());

  }

  public void testServerDefaults3() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<dso-port bind=\"1.2.3.4\">8513</dso-port>" + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;
    String dsoBind = "1.2.3.4";

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    int tempGroupPort = dsoPort + BaseTVSConfigurationSetupManager.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    int tempJmxPort = dsoPort + BaseTVSConfigurationSetupManager.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
    int defaultJmxPort = ((tempJmxPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getL2GroupPort().getBind());

  }

  public void testServerDefaults4() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<dso-port bind=\"1.2.3.4\">8513</dso-port>" + "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    + "<l2-group-port bind=\"5.6.7.8\">7513</l2-group-port>" + "</server>" + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;
    String dsoBind = "1.2.3.4";

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    int jmxPort = 9513;
    String jmxBind = "4.3.2.1";
    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    int l2GroupPort = 7513;
    String l2GroupBind = "5.6.7.8";
    Assert.assertEquals(l2GroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(l2GroupBind, server.getL2GroupPort().getBind());
  }

  public void testServerDefaults5() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<dso-port bind=\"1.2.3.4\">8513</dso-port>" + "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    + "<l2-group-port bind=\"5.6.7.8\">7513</l2-group-port>" + "</server>"
                    + "<server host=\"testHost2\" name=\"server2\" bind=\"4.5.6.7\">"
                    + "<dso-port bind=\"1.2.3.4\">8513</dso-port>" + "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    + "<l2-group-port bind=\"5.6.7.8\">7513</l2-group-port>" + "</server>" + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(2, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getDsoPort().getIntValue(), server
        .getName());

    int dsoPort = 8513;
    String dsoBind = "1.2.3.4";

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    int jmxPort = 9513;
    String jmxBind = "4.3.2.1";
    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    int l2GroupPort = 7513;
    String l2GroupBind = "5.6.7.8";
    Assert.assertEquals(l2GroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(l2GroupBind, server.getL2GroupPort().getBind());

    server = servers.getServerArray(1);
    String host = "testHost2";
    String name = "server2";
    String bind = "4.5.6.7";

    Assert.assertEquals(host, server.getHost());
    Assert.assertEquals(bind, server.getBind());
    Assert.assertEquals(name, server.getName());

    Assert.assertEquals(dsoPort, server.getDsoPort().getIntValue());
    Assert.assertEquals(dsoBind, server.getDsoPort().getBind());

    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    Assert.assertEquals(l2GroupPort, server.getL2GroupPort().getIntValue());
    Assert.assertEquals(l2GroupBind, server.getL2GroupPort().getBind());

  }

  public void testServerDirtctoryDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(new File(BaseTVSConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "data")
        .getAbsolutePath(), server.getData());
    Assert.assertEquals(new File(BaseTVSConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "logs")
        .getAbsolutePath(), server.getLogs());
    Assert.assertEquals(new File(BaseTVSConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + "data-backup").getAbsolutePath(), server.getDataBackup());
    Assert.assertEquals(new File(BaseTVSConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + "statistics").getAbsolutePath(), server.getStatistics());
  }

  public void testServerDirtctoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<data>abc/xyz/123</data>" + "<logs>xyz/abc/451</logs>"
                    + "<data-backup>/qrt/opt/pqr</data-backup>" + "<statistics>/opq/pqr/123/or</statistics>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals("abc/xyz/123", server.getData());
    Assert.assertEquals("xyz/abc/451", server.getLogs());
    Assert.assertEquals("/qrt/opt/pqr", server.getDataBackup());
    Assert.assertEquals("/opq/pqr/123/or", server.getStatistics());
  }

  public void testServerSubsitutedDirtctoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<data>%h</data>" + "<logs>%i</logs>" + "<data-backup>%H</data-backup>"
                    + "<statistics>%n</statistics>" + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostName(), server.getData());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getLogs());
    Assert.assertEquals(System.getProperty("user.home"), server.getDataBackup());
    Assert.assertEquals(System.getProperty("user.name"), server.getStatistics());
  }

  public void testDefaultDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(PersistenceMode.TEMPORARY_SWAP_ONLY, server.getDso().getPersistence().getMode());
    Assert.assertEquals(120, server.getDso().getClientReconnectWindow());
    Assert.assertEquals(true, server.getDso().getGarbageCollection().getEnabled());
    Assert.assertEquals(false, server.getDso().getGarbageCollection().getVerbose());
    Assert.assertEquals(3600, server.getDso().getGarbageCollection().getInterval());
  }

  public void testDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>" + "<dso>"
                    + "<persistence>" + "<mode>permanent-store</mode>" + "</persistence>"
                    + "<client-reconnect-window>9876</client-reconnect-window>" + "<garbage-collection>"
                    + "<enabled>false</enabled>" + "<verbose>true</verbose>" + "<interval>1234</interval>"
                    + "</garbage-collection>" + "</dso>" + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Server server = servers.getServerArray(0);

    Assert.assertEquals(PersistenceMode.PERMANENT_STORE, server.getDso().getPersistence().getMode());
    Assert.assertEquals(9876, server.getDso().getClientReconnectWindow());
    Assert.assertEquals(false, server.getDso().getGarbageCollection().getEnabled());
    Assert.assertEquals(true, server.getDso().getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, server.getDso().getGarbageCollection().getInterval());
  }

  public void testMirrorGroupDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getServerArray().length);
    Assert.assertTrue(servers.isSetMirrorGroups());
    MirrorGroups mirrorGroups = servers.getMirrorGroups();
    Assert.assertEquals(1, mirrorGroups.sizeOfMirrorGroupArray());
    Assert.assertEquals(1, mirrorGroups.getMirrorGroupArray().length);

    Server server = servers.getServerArray(0);
    MirrorGroup mirrorGroup = mirrorGroups.getMirrorGroupArray(0);
    Assert.assertEquals(1, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(1, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(server.getName(), mirrorGroup.getMembers().getMemberArray(0));

    Assert.assertTrue(mirrorGroup.isSetHa());
    Ha defaultHa = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, defaultHa.getMode());
    Assert.assertEquals(5, defaultHa.getNetworkedActivePassive().getElectionTime());
  }

  public void testMirrorGroupWithDefaultHa() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>"
    + "<server host=\"eng01\" name=\"server1\"></server>"
    + "<server host=\"eng02\" name=\"server2\"></server>"
    + "<server host=\"eng03\" name=\"server3\"></server>"
    + "<server host=\"eng04\" name=\"server4\"></server>"
    + "<mirror-groups>"
    + "<mirror-group group-name=\"group1\">"
    + "<members>"
    + "<member>server1</member>"
    + "<member>server2</member>"
    + "</members>"
    + "</mirror-group>"
    + "<mirror-group group-name=\"group2\">"
    + "<members>"
    + "<member>server3</member>"
    + "<member>server4</member>"
    + "</members>"
    + "</mirror-group>"
    + "</mirror-groups>"
    + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(4, servers.getServerArray().length);
    Assert.assertTrue(servers.isSetMirrorGroups());
    MirrorGroups mirrorGroups = servers.getMirrorGroups();
    Assert.assertEquals(2, mirrorGroups.sizeOfMirrorGroupArray());
    Assert.assertEquals(2, mirrorGroups.getMirrorGroupArray().length);

    MirrorGroup mirrorGroup = mirrorGroups.getMirrorGroupArray(0);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(0).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server1", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(1).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server2", mirrorGroup.getMembers().getMemberArray(1));
    
    Assert.assertTrue(mirrorGroup.isSetHa());
    Ha defaultHa = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, defaultHa.getMode());
    Assert.assertEquals(5, defaultHa.getNetworkedActivePassive().getElectionTime());
    
    mirrorGroup = mirrorGroups.getMirrorGroupArray(1);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(2).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server3", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(3).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server4", mirrorGroup.getMembers().getMemberArray(1));

    Assert.assertTrue(mirrorGroup.isSetHa());
    defaultHa = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, defaultHa.getMode());
    Assert.assertEquals(5, defaultHa.getNetworkedActivePassive().getElectionTime());
  }
  
  public void testMirrorGroupWithGivenHa() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>"
    + "<server host=\"eng01\" name=\"server1\"></server>"
    + "<server host=\"eng02\" name=\"server2\"></server>"
    + "<server host=\"eng03\" name=\"server3\"></server>"
    + "<server host=\"eng04\" name=\"server4\"></server>"
    + "<mirror-groups>"
    + "<mirror-group group-name=\"group1\">"
    + "<members>"
    + "<member>server1</member>"
    + "<member>server2</member>"
    + "</members>"
    + "</mirror-group>"
    + "<mirror-group group-name=\"group2\">"
    + "<members>"
    + "<member>server3</member>"
    + "<member>server4</member>"
    + "</members>"
    + "<ha>"
    + "<mode>networked-active-passive</mode>"
    + "<networked-active-passive>"
    + " <election-time>15</election-time>"
    + "</networked-active-passive>"
    + "</ha>"
    + "</mirror-group>"
    + "</mirror-groups>"
    + "<ha>"
    + "<mode>disk-based-active-passive</mode>"
    + "<networked-active-passive>"
    + " <election-time>25</election-time>"
    + "</networked-active-passive>"
    + "</ha>"
    + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(4, servers.getServerArray().length);
    Assert.assertTrue(servers.isSetMirrorGroups());
    MirrorGroups mirrorGroups = servers.getMirrorGroups();
    Assert.assertEquals(2, mirrorGroups.sizeOfMirrorGroupArray());
    Assert.assertEquals(2, mirrorGroups.getMirrorGroupArray().length);

    MirrorGroup mirrorGroup = mirrorGroups.getMirrorGroupArray(0);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(0).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server1", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(1).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server2", mirrorGroup.getMembers().getMemberArray(1));
    
    Assert.assertTrue(mirrorGroup.isSetHa());
    Ha ha = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.DISK_BASED_ACTIVE_PASSIVE, ha.getMode());
    Assert.assertEquals(25, ha.getNetworkedActivePassive().getElectionTime());
    
    mirrorGroup = mirrorGroups.getMirrorGroupArray(1);
    Assert.assertEquals(2, mirrorGroup.getMembers().sizeOfMemberArray());
    Assert.assertEquals(2, mirrorGroup.getMembers().getMemberArray().length);
    Assert.assertEquals(servers.getServerArray(2).getName(), mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals("server3", mirrorGroup.getMembers().getMemberArray(0));
    Assert.assertEquals(servers.getServerArray(3).getName(), mirrorGroup.getMembers().getMemberArray(1));
    Assert.assertEquals("server4", mirrorGroup.getMembers().getMemberArray(1));

    Assert.assertTrue(mirrorGroup.isSetHa());
    ha = mirrorGroup.getHa();
    Assert.assertEquals(HaMode.NETWORKED_ACTIVE_PASSIVE, ha.getMode());
    Assert.assertEquals(15, ha.getNetworkedActivePassive().getElectionTime());
  }
  
  public void testUpdateCheckDefault() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();
    
    Assert.assertTrue(servers.isSetUpdateCheck());
    Assert.assertEquals(true, servers.getUpdateCheck().getEnabled());
    Assert.assertEquals(7, servers.getUpdateCheck().getPeriodDays());
  }

  public void testUpdateCheck() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>"
                    + "<update-check>"
                    + "<enabled>false</enabled>"
                    + "<period-days>14</period-days>"
                    + "</update-check>"
                    + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();
    
    Assert.assertTrue(servers.isSetUpdateCheck());
    Assert.assertEquals(false, servers.getUpdateCheck().getEnabled());
    Assert.assertEquals(14, servers.getUpdateCheck().getPeriodDays());
  }
  
  public void testDefaultClientLogDirectory()throws IOException, ConfigurationSetupException{
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
    + "</server>" + "</servers>" + "<clients>" + "</clients>"+ "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();
    
    Client client = (Client)configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals(new File(ParameterSubstituter.substitute("logs-%i")).getAbsolutePath(), client.getLogs());
  }
  
  public void testClientLogDirectory()throws IOException, ConfigurationSetupException{
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
    + "</server>" + "</servers>" + "<clients>" +"<logs>/abc/xyz/tra</logs>"+ "</clients>"+ "</tc:tc-config>";

    writeConfigFile(config);

    BaseTVSConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager();
    
    Client client = (Client)configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals("/abc/xyz/tra", client.getLogs());
  }
  
  private BaseTVSConfigurationSetupManager initializeAndGetBaseTVSConfigSetupManager()
      throws ConfigurationSetupException {
    String[] args = new String[] { "-f", tcConfig.getAbsolutePath() };

    String effectiveConfigSpec = getEffectiveConfigSpec(
                                                        System
                                                            .getProperty(TVSConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME),
                                                        parseDefaultCommandLine(
                                                                                args,
                                                                                StandardTVSConfigurationSetupManagerFactory.ConfigMode.L2),
                                                        StandardTVSConfigurationSetupManagerFactory.ConfigMode.L2);
    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) { throw new ConfigurationSetupException(
                                                                                  "We can't find the working directory of the process; we need this to continue. "
                                                                                      + "(The system property 'user.dir' was "
                                                                                      + (cwdAsString == null ? "null"
                                                                                          : "'" + cwdAsString + "'")
                                                                                      + ".)"); }
    ConfigurationSpec configurationSpec = new ConfigurationSpec(
                                                                effectiveConfigSpec,
                                                                System
                                                                    .getProperty(TVSConfigurationSetupManagerFactory.SERVER_CONFIG_FILE_PROPERTY_NAME),
                                                                StandardTVSConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new File(cwdAsString));

    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(
                                                                                        configurationSpec,
                                                                                        new TerracottaDomainConfigurationDocumentBeanFactory());

    BaseTVSConfigurationSetupManager configSetupMgr = new BaseTVSConfigurationSetupManager(
                                                                                           configurationCreator,
                                                                                           new FromSchemaDefaultValueProvider(),
                                                                                           new StandardXmlObjectComparator(),
                                                                                           new FatalIllegalConfigurationChangeHandler());
    configSetupMgr.runConfigurationCreator();

    return configSetupMgr;
  }

  protected File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private String getEffectiveConfigSpec(final String configSpec, final CommandLine commandLine,
                                        final ConfigMode configMode) throws ConfigurationSetupException {

    String configFileOnCommandLine = null;
    String effectiveConfigSpec;

    configFileOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('f'));
    effectiveConfigSpec = StringUtils
        .trimToNull(configFileOnCommandLine != null ? configFileOnCommandLine : configSpec);

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      File localConfig = new File(System.getProperty("user.dir"), DEFAULT_CONFIG_SPEC);

      if (localConfig.exists()) {
        effectiveConfigSpec = localConfig.getAbsolutePath();
      } else if (configMode == ConfigMode.L2) {
        effectiveConfigSpec = DEFAULT_CONFIG_URI;
      }
    }

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      // formatting
      throw new ConfigurationSetupException("You must specify the location of the Terracotta "
                                            + "configuration file for this process, using the " + "'"
                                            + CONFIG_FILE_PROPERTY_NAME + "' system property.");
    }

    return effectiveConfigSpec;
  }

  private static CommandLine parseDefaultCommandLine(String[] args, ConfigMode configMode)
      throws ConfigurationSetupException {
    try {
      if (args == null || args.length == 0) {
        return new PosixParser().parse(new Options(), new String[0]);
      } else {
        Options options = createOptions(configMode);

        return new PosixParser().parse(options, args);
      }
    } catch (ParseException pe) {
      throw new ConfigurationSetupException(pe.getLocalizedMessage(), pe);
    }
  }

  private static Options createOptions(ConfigMode configMode) {
    Options options = new Options();

    Option configFileOption = new Option("f", CONFIG_SPEC_ARGUMENT_NAME, true,
                                         "the configuration file to use, specified as a file path or URL");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);

    if (configMode == ConfigMode.L2) {
      configFileOption.setRequired(false);
      options.addOption(configFileOption);

      Option l2NameOption = new Option("n", "name", true, "the name of this L2; defaults to the host name");
      l2NameOption.setRequired(false);
      l2NameOption.setArgName("l2-name");
      options.addOption(l2NameOption);
    } else {
      configFileOption.setRequired(true);
      options.addOption(configFileOption);
    }

    return options;
  }
}
