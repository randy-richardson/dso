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
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory.ConfigMode;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
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

    Assert.assertEquals(BaseTVSConfigurationSetupManager.DEFAULT_DSO_PORT, server.getDsoPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getDsoPort().getBind());

    int tempGroupPort = BaseTVSConfigurationSetupManager.DEFAULT_DSO_PORT
                        + BaseTVSConfigurationSetupManager.DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
    int defaultGroupPort = ((tempGroupPort <= BaseTVSConfigurationSetupManager.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % BaseTVSConfigurationSetupManager.MAX_PORTNUMBER)
          + BaseTVSConfigurationSetupManager.MIN_PORTNUMBER);

    int tempJmxPort = BaseTVSConfigurationSetupManager.DEFAULT_DSO_PORT
                      + BaseTVSConfigurationSetupManager.DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
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

  private BaseTVSConfigurationSetupManager initializeAndGetBaseTVSConfigSetupManager()
      throws ConfigurationSetupException {
    BaseTVSConfigurationSetupManager configSetupMgr = new BaseTVSConfigurationSetupManager(
                                                                                           new FromSchemaDefaultValueProvider(),
                                                                                           new StandardXmlObjectComparator(),
                                                                                           new FatalIllegalConfigurationChangeHandler());

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

    configSetupMgr.runConfigurationCreator(configurationCreator);

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
