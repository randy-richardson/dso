/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.xml.sax.SAXException;

import com.tc.config.schema.beanfactory.BeanWithErrors;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.setup.sources.ConfigurationSource;
import com.tc.config.schema.setup.sources.FileConfigurationSource;
import com.tc.config.schema.setup.sources.ResourceConfigurationSource;
import com.tc.config.schema.setup.sources.ServerConfigurationSource;
import com.tc.config.schema.setup.sources.URLConfigurationSource;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.Client;
import com.terracottatech.config.ConfigurationModel;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.DsoServerData;
import com.terracottatech.config.GarbageCollection;
import com.terracottatech.config.Ha;
import com.terracottatech.config.HaMode;
import com.terracottatech.config.InstrumentationLogging;
import com.terracottatech.config.Members;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.MirrorGroups;
import com.terracottatech.config.Modules;
import com.terracottatech.config.NetworkedActivePassive;
import com.terracottatech.config.Offheap;
import com.terracottatech.config.Persistence;
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.RuntimeLogging;
import com.terracottatech.config.RuntimeOutputOptions;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.UpdateCheck;
import com.terracottatech.config.HaMode.Enum;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A {@link ConfigurationCreator} that works off XML files, using the standard config-spec model.
 */
public class StandardXMLFileConfigurationCreator implements ConfigurationCreator {

  public static final short          DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT   = 10;
  public static final short          DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT = 20;
  public static final int            MIN_PORTNUMBER                        = 0x0FFF;
  public static final int            MAX_PORTNUMBER                        = 0xFFFF;

  private static final TCLogger      consoleLogger                         = CustomerLogging.getConsoleLogger();
  private static final long          GET_CONFIGURATION_TOTAL_TIMEOUT       = 5 * 60 * 1000;
  private static final long          MIN_RETRY_TIMEOUT                     = 5 * 1000;
  private static final Pattern       SERVER_PATTERN                        = Pattern.compile("(.*):(.*)",
                                                                                             Pattern.CASE_INSENSITIVE);
  private static final Pattern       RESOURCE_PATTERN                      = Pattern.compile("resource://(.*)",
                                                                                             Pattern.CASE_INSENSITIVE);
  // We require more than one character before the colon so that we don't mistake Windows-style directory paths as URLs.
  private static final Pattern       URL_PATTERN                           = Pattern.compile("[A-Za-z][A-Za-z]+://.*");
  private static final String        WILDCARD_IP                           = "0.0.0.0";
  private static final long          GET_CONFIGURATION_ONE_SOURCE_TIMEOUT  = TCPropertiesImpl
                                                                               .getProperties()
                                                                               .getLong(
                                                                                        TCPropertiesConsts.TC_CONFIG_SOURCEGET_TIMEOUT,
                                                                                        30000);

  private final ConfigurationSpec    configurationSpec;
  private final ConfigBeanFactory    beanFactory;
  private final TCLogger             logger;

  private boolean                    baseConfigLoadedFromTrustedSource;
  private String                     serverOverrideConfigDescription;
  private boolean                    serverOverrideConfigLoadedFromTrustedSource;
  private File                       directoryLoadedFrom;
  private String                     baseConfigDescription                 = "";
  private volatile String            rawConfigText                         = "";
  private TcConfigDocument           tcConfigDocument;
  private final DefaultValueProvider defaultValueProvider                  = new SchemaDefaultValueProvider();

  public StandardXMLFileConfigurationCreator(final ConfigurationSpec configurationSpec,
                                             final ConfigBeanFactory beanFactory) {
    this(TCLogging.getLogger(StandardXMLFileConfigurationCreator.class), configurationSpec, beanFactory);
  }

  public StandardXMLFileConfigurationCreator(final TCLogger logger, final ConfigurationSpec configurationSpec,
                                             final ConfigBeanFactory beanFactory) {
    Assert.assertNotNull(beanFactory);
    this.logger = logger;
    this.beanFactory = beanFactory;
    this.configurationSpec = configurationSpec;
  }

  public void createConfigurationIntoRepositories(MutableBeanRepository l1BeanRepository,
                                                  MutableBeanRepository l2sBeanRepository,
                                                  MutableBeanRepository systemBeanRepository,
                                                  MutableBeanRepository tcPropertiesRepository,
                                                  ApplicationsRepository applicationsRepository)
      throws ConfigurationSetupException {
    Assert.assertNotNull(l1BeanRepository);
    Assert.assertNotNull(l2sBeanRepository);
    Assert.assertNotNull(systemBeanRepository);
    Assert.assertNotNull(tcPropertiesRepository);
    Assert.assertNotNull(applicationsRepository);

    ConfigurationSource[] sources = getConfigurationSources(this.configurationSpec.getBaseConfigSpec());
    ConfigDataSourceStream baseConfigDataSourceStream = loadConfigDataFromSources(sources, l1BeanRepository,
                                                                                  l2sBeanRepository,
                                                                                  systemBeanRepository,
                                                                                  tcPropertiesRepository,
                                                                                  applicationsRepository);
    baseConfigLoadedFromTrustedSource = baseConfigDataSourceStream.isTrustedSource();
    baseConfigDescription = baseConfigDataSourceStream.getDescription();

    if (this.configurationSpec.shouldOverrideServerTopology()) {
      sources = getConfigurationSources(this.configurationSpec.getServerTopologyOverrideConfigSpec());
      ConfigDataSourceStream serverOverrideConfigDataSourceStream = loadServerConfigDataFromSources(sources,
                                                                                                    l2sBeanRepository,
                                                                                                    true);
      serverOverrideConfigLoadedFromTrustedSource = serverOverrideConfigDataSourceStream.isTrustedSource();
      serverOverrideConfigDescription = serverOverrideConfigDataSourceStream.getDescription();
    }
    logCopyOfConfig();
  }

  public void reloadServersConfiguration(MutableBeanRepository l2sBeanRepository, boolean shouldLogTcConfig,
                                         boolean reportToConsole) throws ConfigurationSetupException {
    ConfigurationSource[] sources = getConfigurationSources(this.configurationSpec.getBaseConfigSpec());
    if (this.configurationSpec.shouldOverrideServerTopology()) {
      sources = getConfigurationSources(this.configurationSpec.getServerTopologyOverrideConfigSpec());
      ConfigDataSourceStream serverOverrideConfigDataSourceStream = loadServerConfigDataFromSources(sources,
                                                                                                    l2sBeanRepository,
                                                                                                    reportToConsole);
      serverOverrideConfigLoadedFromTrustedSource = serverOverrideConfigDataSourceStream.isTrustedSource();
      serverOverrideConfigDescription = serverOverrideConfigDataSourceStream.getDescription();
    } else {
      loadServerConfigDataFromSources(sources, l2sBeanRepository, reportToConsole);
    }

    if (shouldLogTcConfig) {
      logCopyOfConfig();
    }
  }

  private ConfigurationSource[] getConfigurationSources(String configrationSpec) throws ConfigurationSetupException {
    String[] components = configrationSpec.split(",");
    ConfigurationSource[] out = new ConfigurationSource[components.length];

    for (int i = 0; i < components.length; ++i) {
      String thisComponent = components[i];
      ConfigurationSource source = attemptToCreateServerSource(thisComponent);

      if (source == null) source = attemptToCreateResourceSource(thisComponent);
      if (source == null) source = attemptToCreateURLSource(thisComponent);
      if (source == null) source = attemptToCreateFileSource(thisComponent);

      if (source == null) {
        // formatting
        throw new ConfigurationSetupException("The location '" + thisComponent
                                              + "' is not in any recognized format -- it doesn't "
                                              + "seem to be a server, resource, URL, or file.");
      }

      out[i] = source;
    }

    return out;
  }

  private ConfigurationSource attemptToCreateServerSource(String text) {
    Matcher matcher = SERVER_PATTERN.matcher(text);
    if (matcher.matches()) {
      String host = matcher.group(1);
      String portText = matcher.group(2);

      try {
        return new ServerConfigurationSource(host.trim(), Integer.parseInt(portText.trim()));
      } catch (Exception e) {/**/
      }
    }
    return null;
  }

  private ConfigurationSource attemptToCreateResourceSource(String text) {
    Matcher matcher = RESOURCE_PATTERN.matcher(text);
    if (matcher.matches()) return new ResourceConfigurationSource(matcher.group(1), getClass());
    else return null;
  }

  private ConfigurationSource attemptToCreateFileSource(String text) {
    return new FileConfigurationSource(text, this.configurationSpec.getWorkingDir());
  }

  private ConfigurationSource attemptToCreateURLSource(String text) {
    Matcher matcher = URL_PATTERN.matcher(text);
    if (matcher.matches()) return new URLConfigurationSource(text);
    else return null;
  }

  private ConfigDataSourceStream loadConfigDataFromSources(ConfigurationSource[] sources,
                                                           MutableBeanRepository l1BeanRepository,
                                                           MutableBeanRepository l2sBeanRepository,
                                                           MutableBeanRepository systemBeanRepository,
                                                           MutableBeanRepository tcPropertiesRepository,
                                                           ApplicationsRepository applicationsRepository)
      throws ConfigurationSetupException {
    long startTime = java.lang.System.currentTimeMillis();
    ConfigDataSourceStream configDataSourceStream = getConfigDataSourceStrean(sources, startTime, "base configuration");
    if (configDataSourceStream.getSourceInputStream() == null) configurationFetchFailed(sources, startTime);
    loadConfigurationData(configDataSourceStream.getSourceInputStream(), configDataSourceStream.isTrustedSource(),
                          configDataSourceStream.getDescription(), l1BeanRepository, l2sBeanRepository,
                          systemBeanRepository, tcPropertiesRepository, applicationsRepository);
    consoleLogger.info("Successfully loaded " + configDataSourceStream.getDescription() + ".");
    return configDataSourceStream;
  }

  private ConfigDataSourceStream loadServerConfigDataFromSources(ConfigurationSource[] sources,
                                                                 MutableBeanRepository l2sBeanRepository,
                                                                 boolean reportToConsole)
      throws ConfigurationSetupException {
    long startTime = java.lang.System.currentTimeMillis();
    ConfigDataSourceStream configDataSourceStream = getConfigDataSourceStrean(sources, startTime, "server topology");
    if (configDataSourceStream.getSourceInputStream() == null) configurationFetchFailed(sources, startTime);
    loadServerConfigurationData(configDataSourceStream.getSourceInputStream(),
                                configDataSourceStream.isTrustedSource(), configDataSourceStream.getDescription(),
                                l2sBeanRepository);
    if (reportToConsole) {
      consoleLogger.info("Successfully overridden " + configDataSourceStream.getDescription() + ".");
    }
    return configDataSourceStream;
  }

  private static class ConfigDataSourceStream {
    private final InputStream sourceInputStream;
    private final boolean     trustedSource;
    private final String      description;

    public ConfigDataSourceStream(InputStream sourceInputStream, boolean trustedSource, String description) {
      this.sourceInputStream = sourceInputStream;
      this.trustedSource = trustedSource;
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    public InputStream getSourceInputStream() {
      return sourceInputStream;
    }

    public boolean isTrustedSource() {
      return trustedSource;
    }
  }

  private ConfigDataSourceStream getConfigDataSourceStrean(ConfigurationSource[] sources, long startTime,
                                                           String description) {
    ConfigurationSource[] remainingSources = new ConfigurationSource[sources.length];
    ConfigurationSource loadedSource = null;
    java.lang.System.arraycopy(sources, 0, remainingSources, 0, sources.length);
    long lastLoopStartTime = 0;
    int iteration = 0;
    InputStream out = null;
    boolean trustedSource = false;
    String descrip = null;

    while (iteration == 0 || (java.lang.System.currentTimeMillis() - startTime < GET_CONFIGURATION_TOTAL_TIMEOUT)) {
      sleepIfNecessaryToAvoidPoundingSources(lastLoopStartTime);
      lastLoopStartTime = java.lang.System.currentTimeMillis();

      for (int i = 0; i < remainingSources.length; ++i) {

        if (remainingSources[i] == null) continue;
        out = trySource(remainingSources, i);

        if (out != null) {
          loadedSource = remainingSources[i];
          trustedSource = loadedSource.isTrusted();
          descrip = description + " from " + loadedSource.toString();
          break;
        }
      }

      if (out != null) break;
      ++iteration;
      boolean haveSources = false;
      for (int i = 0; i < remainingSources.length; ++i)
        haveSources = haveSources || remainingSources[i] != null;
      if (!haveSources) {
        // All sources have failed; bail out.
        break;
      }
    }
    return new ConfigDataSourceStream(out, trustedSource, descrip);
  }

  private void configurationFetchFailed(ConfigurationSource[] sources, long startTime)
      throws ConfigurationSetupException {
    String text = "Could not fetch configuration data from ";
    if (sources.length > 1) text += "" + sources.length + " different configuration sources";
    else text += "the " + sources[0];
    text += ". ";

    if (sources.length > 1) {
      text += " The sources we tried are: ";
      for (int i = 0; i < sources.length; ++i) {
        if (i > 0) text += ", ";
        if (i == sources.length - 1) text += "and ";
        text += "the " + sources[i].toString();
      }
      text += ". ";
    }

    if (java.lang.System.currentTimeMillis() - startTime >= GET_CONFIGURATION_TOTAL_TIMEOUT) {
      text += " Fetch attempt duration: " + ((java.lang.System.currentTimeMillis() - startTime) / 1000) + " seconds.";
    }

    text += "\n\nTo correct this problem specify a valid configuration location using the ";
    text += "-f/--config command-line options.";

    consoleLogger.error(text);
    throw new ConfigurationSetupException(text);
  }

  private InputStream trySource(ConfigurationSource[] remainingSources, int i) {
    InputStream out = null;

    try {
      logger.info("Attempting to load configuration from the " + remainingSources[i] + "...");
      out = remainingSources[i].getInputStream(GET_CONFIGURATION_ONE_SOURCE_TIMEOUT);
      directoryLoadedFrom = remainingSources[i].directoryLoadedFrom();
    } catch (ConfigurationSetupException cse) {
      String text = "We couldn't load configuration data from the " + remainingSources[i];
      text += "; this error is permanent, so this source will not be retried.";

      if (remainingSources.length > 1) text += " Skipping this source and going to the next one.";

      text += " (Error: " + cse.getLocalizedMessage() + ".)";

      consoleLogger.warn(text);

      remainingSources[i] = null;
    } catch (IOException ioe) {
      String text = "We couldn't load configuration data from the " + remainingSources[i];

      if (remainingSources.length > 1) {
        text += "; this error is temporary, so this source will be retried later if configuration can't be loaded elsewhere. ";
        text += "Skipping this source and going to the next one.";
      } else {
        text += "; retrying.";
      }

      text += " (Error: " + ioe.getLocalizedMessage() + ".)";
      consoleLogger.warn(text);
    }

    return out;
  }

  private void sleepIfNecessaryToAvoidPoundingSources(long lastLoopStartTime) {
    long delay = MIN_RETRY_TIMEOUT - (java.lang.System.currentTimeMillis() - lastLoopStartTime);
    if (delay > 0) {
      logger.info("Waiting " + delay + " ms until we try to get configuration data again...");
      ThreadUtil.reallySleep(delay);
    }
  }

  private void updateTcConfigFull(TcConfigDocument configDocument, String description) {
    updateTcConfig(configDocument, description, false);
  }

  private void updateTcConfigServerElements(TcConfigDocument configDocument, String description) {
    updateTcConfig(configDocument, description, true);
  }

  private void updateTcConfig(TcConfigDocument configDocument, String description, boolean serverElementsOnly) {
    if (!serverElementsOnly) {
      this.tcConfigDocument = (TcConfigDocument) configDocument.copy();
    } else {
      Assert.assertNotNull(this.tcConfigDocument);
      TcConfig toConfig = this.tcConfigDocument.getTcConfig();
      TcConfig fromConfig = configDocument.getTcConfig();
      if (toConfig.getServers() != null) toConfig.setServers(fromConfig.getServers());
    }
    rawConfigText = this.tcConfigDocument.toString();
  }

  private void logCopyOfConfig() {
    logger.info(describeSources() + ":\n\n" + rawConfigText);
  }

  private void loadConfigurationData(InputStream in, boolean trustedSource, String descrip,
                                     MutableBeanRepository clientBeanRepository,
                                     MutableBeanRepository serversBeanRepository,
                                     MutableBeanRepository systemBeanRepository,
                                     MutableBeanRepository tcPropertiesRepository,
                                     ApplicationsRepository applicationsRepository) throws ConfigurationSetupException {
    try {

      TcConfigDocument configDocument = getConfigFromSourceStream(in, trustedSource, descrip);
      Assert.assertNotNull(configDocument);
      updateTcConfigFull(configDocument, descrip);
      setClientBean(clientBeanRepository, configDocument.getTcConfig(), descrip);
      setServerBean(serversBeanRepository, configDocument.getTcConfig(), descrip);
      setSystemBean(systemBeanRepository, configDocument.getTcConfig(), descrip);
      setTcPropertiesBean(tcPropertiesRepository, configDocument.getTcConfig(), descrip);
      setApplicationsBean(applicationsRepository, configDocument.getTcConfig(), descrip);
    } catch (XmlException xmle) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the "
                                            + "Terracotta schema: " + xmle.getLocalizedMessage(), xmle);
    }
  }

  private void loadServerConfigurationData(InputStream in, boolean trustedSource, String descrip,
                                           MutableBeanRepository serversBeanRepository)
      throws ConfigurationSetupException {
    try {
      TcConfigDocument configDocument = getConfigFromSourceStream(in, trustedSource, descrip);
      Assert.assertNotNull(configDocument);
      updateTcConfigServerElements(configDocument, descrip);
      setServerBean(serversBeanRepository, configDocument.getTcConfig(), descrip);
    } catch (XmlException xmle) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the "
                                            + "Terracotta schema: " + xmle.getLocalizedMessage(), xmle);
    }
  }

  private void setClientBean(MutableBeanRepository clientBeanRepository, TcConfig config, String description)
      throws XmlException {
    clientBeanRepository.setBean(config.getClients(), description);
  }

  private void setServerBean(MutableBeanRepository serversBeanRepository, TcConfig config, String description)
      throws XmlException {
    serversBeanRepository.setBean(config.getServers(), description);
  }

  private void setSystemBean(MutableBeanRepository systemBeanRepository, TcConfig config, String description)
      throws XmlException {
    systemBeanRepository.setBean(config.getSystem(), description);
  }

  private void setTcPropertiesBean(MutableBeanRepository tcPropertiesRepository, TcConfig config, String description)
      throws XmlException {
    tcPropertiesRepository.setBean(config.getTcProperties(), description);
  }

  private void setApplicationsBean(ApplicationsRepository applicationsRepository, TcConfig config, String description)
      throws XmlException {
    if (config.isSetApplication()) {
      applicationsRepository.repositoryFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME)
          .setBean(config.getApplication(), description);
    }
  }

  private TcConfigDocument getConfigFromSourceStream(InputStream in, boolean trustedSource, String descrip)
      throws ConfigurationSetupException {
    TcConfigDocument tcConfigDoc;
    try {
      ByteArrayOutputStream dataCopy = new ByteArrayOutputStream();
      IOUtils.copy(in, dataCopy);
      in.close();

      InputStream copyIn = new ByteArrayInputStream(dataCopy.toByteArray());
      BeanWithErrors beanWithErrors = beanFactory.createBean(copyIn, descrip);

      if (beanWithErrors.errors() != null && beanWithErrors.errors().length > 0) {
        logger.debug("Configuration didn't parse; it had " + beanWithErrors.errors().length + " error(s).");

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < beanWithErrors.errors().length; ++i) {
          XmlError error = beanWithErrors.errors()[i];
          buf.append("  [" + i + "]: Line " + error.getLine() + ", column " + error.getColumn() + ": "
                     + error.getMessage() + "\n");
          if (error.getMessage().indexOf("spring") > -1) {
            buf
                .append("  The Spring configuration in your Terracotta configuration file is not valid. "
                        + "Clustering Spring no longer requires special configuration. For more information, see http://www.terracotta.org/spring.\n");
          }
        }

        throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the "
                                              + "Terracotta schema:\n" + buf);
      } else {
        logger.debug("Configuration is valid.");
      }

      tcConfigDoc = ((TcConfigDocument) beanWithErrors.bean());
      TcConfig config = tcConfigDoc.getTcConfig();
      initializeSystem(config);
      initializeServers(config);
      initializeClients(config);
    } catch (IOException ioe) {
      throw new ConfigurationSetupException("We were unable to read configuration data from the " + descrip + ": "
                                            + ioe.getLocalizedMessage(), ioe);
    } catch (SAXException saxe) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " is not well-formed XML: "
                                            + saxe.getLocalizedMessage(), saxe);
    } catch (ParserConfigurationException pce) {
      throw Assert.failure("The XML parser can't be configured correctly; this should not happen.", pce);
    } catch (XmlException xmle) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the "
                                            + "Terracotta schema: " + xmle.getLocalizedMessage(), xmle);
    }
    return tcConfigDoc;
  }

  private void initializeSystem(TcConfig config) throws XmlException {
    System system;
    if (!config.isSetSystem()) {
      system = config.addNewSystem();
    } else {
      system = config.getSystem();
    }
    initializeConfigurationModel(system);
  }

  private void initializeConfigurationModel(System system) throws XmlException {
    if (system != null && !system.isSetConfigurationModel()) {
      system.setConfigurationModel(getDefaultSystemConfigurationModel(system));
    }
  }

  private ConfigurationModel.Enum getDefaultSystemConfigurationModel(System system) throws XmlException {
    XmlString defaultValue = (XmlString) this.defaultValueProvider.defaultFor(system.schemaType(),
                                                                              "configuration-model");
    Assert.assertNotNull(defaultValue);
    Assert.assertTrue(defaultValue.getStringValue().equals(ConfigurationModel.DEVELOPMENT.toString())
                      || defaultValue.getStringValue().equals(ConfigurationModel.PRODUCTION.toString()));

    if (defaultValue.getStringValue().equals(ConfigurationModel.PRODUCTION.toString())) return ConfigurationModel.PRODUCTION;
    return ConfigurationModel.DEVELOPMENT;
  }

  private void initializeServers(TcConfig config) throws XmlException, ConfigurationSetupException {
    Servers servers = config.getServers();

    if (servers == null) {
      servers = config.addNewServers();
      servers.addNewServer();
    }

    for (int i = 0; i < servers.sizeOfServerArray(); i++) {
      Server server = servers.getServerArray(i);
      initializeServerBind(server);
      initializeDsoPort(server);
      initializeJmxPort(server);
      initializeL2GroupPort(server);
      // CDV-1220: per our documentation in the schema itself, host is supposed to default to server name or '%i'
      // and name is supposed to default to 'host:dso-port'
      initializeNameAndHost(server);
      initializeDataDirectory(server);
      initializeLogsDirectory(server);
      initializeDataBackupDirectory(server);
      initializeStatisticsDirectory(server);
      initializeDso(server);
    }

    initializeMirrorGroups(servers);
    initializeUpdateCheck(servers);
  }

  private void initializeServerBind(Server server) {
    if (!server.isSetBind() || server.getBind().trim().length() == 0) {
      server.setBind(WILDCARD_IP);
    }
    server.setBind(ParameterSubstituter.substitute(server.getBind()));
  }

  private void initializeDsoPort(Server server) throws XmlException {
    XmlObject[] dsoPorts = server.selectPath("dso-port");
    Assert.assertTrue(dsoPorts.length <= 1);
    if (!server.isSetDsoPort()) {
      final XmlInteger defaultValue = (XmlInteger) this.defaultValueProvider
          .defaultFor(server.schemaType(), "dso-port");
      int defaultDsoPort = defaultValue.getBigIntegerValue().intValue();
      BindPort dsoPort = server.addNewDsoPort();
      dsoPort.setIntValue(defaultDsoPort);
      dsoPort.setBind(server.getBind());
    } else if (!server.getDsoPort().isSetBind()) {
      server.getDsoPort().setBind(server.getBind());
    }
  }

  private void initializeJmxPort(Server server) {
    XmlObject[] jmxPorts = server.selectPath("jmx-port");
    Assert.assertTrue(jmxPorts.length <= 1);
    if (!server.isSetJmxPort()) {
      BindPort jmxPort = server.addNewJmxPort();
      int tempJmxPort = server.getDsoPort().getIntValue() + DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT;
      int defaultJmxPort = ((tempJmxPort <= MAX_PORTNUMBER) ? tempJmxPort : (tempJmxPort % MAX_PORTNUMBER)
                                                                            + MIN_PORTNUMBER);

      jmxPort.setIntValue(defaultJmxPort);
      jmxPort.setBind(server.getBind());
    } else if (!server.getJmxPort().isSetBind()) {
      server.getJmxPort().setBind(server.getBind());
    }
  }

  private void initializeL2GroupPort(Server server) {
    XmlObject[] l2GroupPorts = server.selectPath("l2-group-port");
    Assert.assertTrue(l2GroupPorts.length <= 1);
    if (!server.isSetL2GroupPort()) {
      BindPort l2GrpPort = server.addNewL2GroupPort();
      int tempGroupPort = server.getDsoPort().getIntValue() + DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT;
      int defaultGroupPort = ((tempGroupPort <= MAX_PORTNUMBER) ? (tempGroupPort) : (tempGroupPort % MAX_PORTNUMBER)
                                                                                    + MIN_PORTNUMBER);
      l2GrpPort.setIntValue(defaultGroupPort);
      l2GrpPort.setBind(server.getBind());
    } else if (!server.getL2GroupPort().isSetBind()) {
      server.getL2GroupPort().setBind(server.getBind());
    }
  }

  private void initializeNameAndHost(Server server) {
    if (!server.isSetHost() || server.getHost().trim().length() == 0) {
      if (!server.isSetName()) {
        server.setHost("%i");
      } else {
        server.setHost(server.getName());
      }
    }

    if (!server.isSetName() || server.getName().trim().length() == 0) {
      int dsoPort = server.getDsoPort().getIntValue();
      server.setName(server.getHost() + (dsoPort > 0 ? ":" + dsoPort : ""));
    }

    // CDV-77: add parameter expansion to the <server> attributes ('host' and 'name')
    server.setHost(ParameterSubstituter.substitute(server.getHost()));
    server.setName(ParameterSubstituter.substitute(server.getName()));
  }

  private void initializeDataDirectory(Server server) throws XmlException {
    if (!server.isSetData()) {
      final XmlString defaultValue = (XmlString) this.defaultValueProvider.defaultFor(server.schemaType(), "data");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());

      server.setData(new File(this.directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server.setData(ParameterSubstituter.substitute(server.getData()));
    }
  }

  private void initializeLogsDirectory(Server server) throws XmlException {
    if (!server.isSetLogs()) {
      final XmlString defaultValue = (XmlString) this.defaultValueProvider.defaultFor(server.schemaType(), "logs");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());
      server.setLogs(new File(this.directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server.setLogs(ParameterSubstituter.substitute(server.getLogs()));
    }
  }

  private void initializeDataBackupDirectory(Server server) throws XmlException {
    if (!server.isSetDataBackup()) {
      final XmlString defaultValue = (XmlString) this.defaultValueProvider.defaultFor(server.schemaType(),
                                                                                      "data-backup");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());
      server.setDataBackup(new File(this.directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server.setDataBackup(ParameterSubstituter.substitute(server.getDataBackup()));
    }
  }

  private void initializeStatisticsDirectory(Server server) throws XmlException {
    if (!server.isSetStatistics()) {
      final XmlString defaultValue = (XmlString) this.defaultValueProvider
          .defaultFor(server.schemaType(), "statistics");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());
      server.setStatistics(new File(this.directoryLoadedFrom, substitutedString).getAbsolutePath());
    } else {
      server.setStatistics(ParameterSubstituter.substitute(server.getStatistics()));
    }
  }

  private void initializeDso(Server server) throws XmlException {
    if (!server.isSetDso()) {
      DsoServerData dso = server.addNewDso();
      initializeDefaultPersistence(server);
      dso.setClientReconnectWindow(getDefaultReconnectWindow(server));
      initializeDefaultGarbageCollection(server);
    } else {
      DsoServerData dso = server.getDso();

      if (!dso.isSetPersistence()) {
        dso.addNewPersistence().setMode(getDefaultPersistence(server));
        initializeDefaultOffHeap(server);
      }else{
        Persistence persistence = dso.getPersistence();
        if(!persistence.isSetMode()){
          persistence.setMode(getDefaultPersistence(server));
        }
        
        if(!persistence.isSetOffheap()){
          initializeDefaultOffHeap(server);
        }else{
          Offheap offHeap = persistence.getOffheap();
          if(!offHeap.isSetEnabled()){
            offHeap.setEnabled(getDefaultOffHeapEnabled(server));
          }
          
          if(!offHeap.isSetMaxDataSize()){
            offHeap.setMaxDataSize(getDefaultOffHeapMaxDataSize(server));
          }
        }
      }

      if (!dso.isSetClientReconnectWindow()) {
        dso.setClientReconnectWindow(getDefaultReconnectWindow(server));
      }

      if (!dso.isSetGarbageCollection()) {
        initializeDefaultGarbageCollection(server);
      } else {
        GarbageCollection gc = dso.getGarbageCollection();
        if (!gc.isSetEnabled()) {
          gc.setEnabled(getDefaultGarbageCollectionEnabled(server));
        }

        if (!gc.isSetVerbose()) {
          gc.setVerbose(getDefaultGarbageCollectionVerbose(server));
        }

        if (!gc.isSetInterval()) {
          gc.setInterval(getDefaultGarbageCollectionInterval(server));
        }
      }
    }
  }

  private void initializeDefaultPersistence(Server server) throws XmlException {
    Assert.assertTrue(server.isSetDso());
    Assert.assertFalse(server.getDso().isSetPersistence());
    Persistence persistence = server.getDso().addNewPersistence();

    persistence.setMode(getDefaultPersistence(server));
    initializeDefaultOffHeap(server);
  }

  private void initializeDefaultOffHeap(Server server) throws XmlException {
    Assert.assertTrue(server.isSetDso());
    Assert.assertTrue(server.getDso().isSetPersistence());
    Offheap offHeap = server.getDso().getPersistence().addNewOffheap();
    offHeap.setEnabled(getDefaultOffHeapEnabled(server));
    offHeap.setMaxDataSize(getDefaultOffHeapMaxDataSize(server));
  }

  private void initializeDefaultGarbageCollection(Server server) throws XmlException {
    Assert.assertTrue(server.isSetDso());
    Assert.assertFalse(server.getDso().isSetGarbageCollection());
    
    GarbageCollection gc = server.getDso().addNewGarbageCollection();
    gc.setEnabled(getDefaultGarbageCollectionEnabled(server));
    gc.setVerbose(getDefaultGarbageCollectionVerbose(server));
    gc.setInterval(getDefaultGarbageCollectionInterval(server));
  }

  private PersistenceMode.Enum getDefaultPersistence(Server server) throws XmlException {
    XmlString xmlObject = (XmlString) this.defaultValueProvider.defaultFor(server.schemaType(), "dso/persistence/mode");
    Assert.assertNotNull(xmlObject);
    Assert.assertTrue(xmlObject.getStringValue().equals(PersistenceMode.PERMANENT_STORE.toString())
                      || xmlObject.getStringValue().equals(PersistenceMode.TEMPORARY_SWAP_ONLY.toString()));
    if (xmlObject.getStringValue().equals(PersistenceMode.PERMANENT_STORE.toString())) return PersistenceMode.PERMANENT_STORE;
    return PersistenceMode.TEMPORARY_SWAP_ONLY;
  }
  
  private boolean getDefaultOffHeapEnabled(Server server) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(server.schemaType(), "dso/persistence/offheap/enabled"))
    .getBooleanValue();
  }
  
  private String getDefaultOffHeapMaxDataSize(Server server) throws XmlException {
    return ((XmlString) this.defaultValueProvider
        .defaultFor(server.schemaType(), "dso/persistence/offheap/maxDataSize")).getStringValue();
  }

  private int getDefaultReconnectWindow(Server server) throws XmlException {
    return ((XmlInteger) this.defaultValueProvider.defaultFor(server.schemaType(), "dso/client-reconnect-window"))
        .getBigIntegerValue().intValue();
  }

  private boolean getDefaultGarbageCollectionEnabled(Server server) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(server.schemaType(), "dso/garbage-collection/enabled"))
        .getBooleanValue();
  }

  private boolean getDefaultGarbageCollectionVerbose(Server server) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(server.schemaType(), "dso/garbage-collection/verbose"))
        .getBooleanValue();
  }

  private int getDefaultGarbageCollectionInterval(Server server) throws XmlException {
    return ((XmlInteger) this.defaultValueProvider.defaultFor(server.schemaType(), "dso/garbage-collection/interval"))
        .getBigIntegerValue().intValue();
  }

  private void initializeMirrorGroups(Servers servers) throws ConfigurationSetupException {
    if (!servers.isSetMirrorGroups()) {
      createDefaultServerMirrorGroups(servers);
    } else {
      MirrorGroup[] mirrorGroups = servers.getMirrorGroups().getMirrorGroupArray();
      for (MirrorGroup mirrorGroup : mirrorGroups) {
        if (!mirrorGroup.isSetHa()) {
          Ha ha;
          try {
            ha = servers.isSetHa() ? servers.getHa() : getDefaultCommonHa(servers);
          } catch (XmlException e) {
            throw new ConfigurationSetupException(e);
          }
          mirrorGroup.setHa(ha);
        }
      }
    }
  }

  private void createDefaultServerMirrorGroups(Servers servers) throws ConfigurationSetupException {
    Ha ha;
    try {
      ha = servers.isSetHa() ? servers.getHa() : getDefaultCommonHa(servers);
    } catch (XmlException e) {
      throw new ConfigurationSetupException(e);
    }
    MirrorGroups mirrorGroups = servers.addNewMirrorGroups();
    MirrorGroup mirrorGroup = mirrorGroups.addNewMirrorGroup();
    mirrorGroup.setHa(ha);
    Members members = mirrorGroup.addNewMembers();
    Server[] serverArray = servers.getServerArray();

    for (int i = 0; i < serverArray.length; i++) {
      // name for each server should exist
      String name = serverArray[i].getName();
      if (name == null || name.equals("")) { throw new ConfigurationSetupException(
                                                                                   "server's name not defined... name=["
                                                                                       + name + "] serverDsoPort=["
                                                                                       + serverArray[i].getDsoPort()
                                                                                       + "]"); }
      members.insertMember(i, serverArray[i].getName());
    }
  }

  private void initializeUpdateCheck(Servers servers) throws ConfigurationSetupException {
    if (!servers.isSetUpdateCheck()) {
      try {
        servers.setUpdateCheck(getDefaultUpdateCheck(servers));
      } catch (XmlException e) {
        throw new ConfigurationSetupException(e);
      }
    }
  }

  private Ha getDefaultCommonHa(Servers servers) throws XmlException {
    final int defaultElectionTime = ((XmlInteger) defaultValueProvider
        .defaultFor(servers.schemaType(), "ha/networked-active-passive/election-time")).getBigIntegerValue().intValue();
    final String defaultHaModeString = ((XmlString) defaultValueProvider.defaultFor(servers.schemaType(), "ha/mode"))
        .getStringValue();
    final Enum defaultHaMode;
    if (HaMode.DISK_BASED_ACTIVE_PASSIVE.toString().equals(defaultHaModeString)) {
      defaultHaMode = HaMode.DISK_BASED_ACTIVE_PASSIVE;
    } else {
      defaultHaMode = HaMode.NETWORKED_ACTIVE_PASSIVE;
    }

    Ha ha = Ha.Factory.newInstance();
    ha.setMode(defaultHaMode);
    NetworkedActivePassive nap = NetworkedActivePassive.Factory.newInstance();
    nap.setElectionTime(defaultElectionTime);
    ha.setNetworkedActivePassive(nap);
    return ha;
  }

  private UpdateCheck getDefaultUpdateCheck(Servers servers) throws XmlException {
    final int defaultPeriodDays = ((XmlInteger) defaultValueProvider.defaultFor(servers.schemaType(),
                                                                                "update-check/period-days"))
        .getBigIntegerValue().intValue();
    final boolean defaultEnabled = ((XmlBoolean) defaultValueProvider.defaultFor(servers.schemaType(),
                                                                                 "update-check/enabled"))
        .getBooleanValue();
    UpdateCheck uc = UpdateCheck.Factory.newInstance();
    uc.setEnabled(defaultEnabled);
    uc.setPeriodDays(defaultPeriodDays);
    return uc;
  }

  private void initializeClients(TcConfig config) throws XmlException {
    Client client;
    if (!config.isSetClients()) {
      client = config.addNewClients();
    } else {
      client = config.getClients();
    }
    initializeLogsDirectory(client);
    initializeModules(client);
    initiailizeDsoClient(client);
  }

  private void initializeLogsDirectory(Client client) throws XmlException {
    if (client != null && !client.isSetLogs()) {
      final XmlString defaultValue = (XmlString) this.defaultValueProvider.defaultFor(client.schemaType(), "logs");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());

      client.setLogs(new File(substitutedString).getAbsolutePath());
    }
  }

  private void initializeModules(Client client) {
    if (client != null && client.isSetModules()) {
      Modules modules = client.getModules();
      for (int i = 0; i < modules.sizeOfRepositoryArray(); i++) {
        String location = modules.getRepositoryArray(i);
        modules.setRepositoryArray(i, ParameterSubstituter.substitute(location));
      }
    }
  }

  private void initiailizeDsoClient(Client client) throws XmlException {
    if (!client.isSetDso()) {
      DsoClientData dsoClientData = client.addNewDso();
      dsoClientData.setFaultCount(getDefaultFaultCount(client));

      DsoClientDebugging debugging = dsoClientData.addNewDebugging();
      addDefaultInstrumentationLogging(client, debugging);
      addDefaultRuntimeLogging(client, debugging);
      addDefaultRuntimeOutputOptions(client, debugging);
    } else {
      DsoClientData dsoClientData = client.getDso();
      if (!dsoClientData.isSetFaultCount()) {
        dsoClientData.setFaultCount(getDefaultFaultCount(client));
      }

      if (!dsoClientData.isSetDebugging()) {
        DsoClientDebugging debugging = dsoClientData.addNewDebugging();
        addDefaultInstrumentationLogging(client, debugging);
        addDefaultRuntimeLogging(client, debugging);
      } else {
        DsoClientDebugging debugging = dsoClientData.getDebugging();
        if (!debugging.isSetInstrumentationLogging()) {
          addDefaultInstrumentationLogging(client, debugging);
        } else {
          checkAndSetInstrumentationLogging(client, debugging.getInstrumentationLogging());
        }

        if (!debugging.isSetRuntimeLogging()) {
          addDefaultRuntimeLogging(client, debugging);
        } else {
          checkAndSetRuntimeLogging(client, debugging.getRuntimeLogging());
        }

        if (!debugging.isSetRuntimeOutputOptions()) {
          addDefaultRuntimeOutputOptions(client, debugging);
        } else {
          checkAndSetRuntimeOutputOptions(client, debugging.getRuntimeOutputOptions());
        }
      }
    }
  }

  private void addDefaultInstrumentationLogging(Client client, DsoClientDebugging debugging) throws XmlException {
    checkAndSetInstrumentationLogging(client, debugging.addNewInstrumentationLogging());
  }

  private void addDefaultRuntimeLogging(Client client, DsoClientDebugging debugging) throws XmlException {
    checkAndSetRuntimeLogging(client, debugging.addNewRuntimeLogging());
  }

  private void addDefaultRuntimeOutputOptions(Client client, DsoClientDebugging debugging) throws XmlException {
    checkAndSetRuntimeOutputOptions(client, debugging.addNewRuntimeOutputOptions());
  }

  private void checkAndSetInstrumentationLogging(Client client, InstrumentationLogging instrumentationLogging)
      throws XmlException {
    if (!instrumentationLogging.isSetClass1()) {
      instrumentationLogging.setClass1(getDefaultClassInstrumentationLogging(client));
    }

    if (!instrumentationLogging.isSetHierarchy()) {
      instrumentationLogging.setHierarchy(getDefaultHierarchyInstrumentationLogging(client));
    }

    if (!instrumentationLogging.isSetLocks()) {
      instrumentationLogging.setLocks(getDefaultLocksInstrumentationLoggings(client));
    }

    if (!instrumentationLogging.isSetTransientRoot()) {
      instrumentationLogging.setTransientRoot(getDefaultTransientRootInstrumentationLogging(client));
    }

    if (!instrumentationLogging.isSetRoots()) {
      instrumentationLogging.setRoots(getDefaultRootsInstrumentationLogging(client));
    }

    if (!instrumentationLogging.isSetDistributedMethods()) {
      instrumentationLogging.setDistributedMethods(getDefaultDistributedMethodInstrumentationLogging(client));
    }
  }

  private void checkAndSetRuntimeLogging(Client client, RuntimeLogging runtimeLogging) throws XmlException {
    if (!runtimeLogging.isSetNonPortableDump()) {
      runtimeLogging.setNonPortableDump(getDefaultNonPortableDumpRuntimeLogging(client));
    }

    if (!runtimeLogging.isSetLockDebug()) {
      runtimeLogging.setLockDebug(getDefaultLockDebugRuntimeLogging(client));
    }

    if (!runtimeLogging.isSetFieldChangeDebug()) {
      runtimeLogging.setFieldChangeDebug(getDefaultFieldChangeDebugRuntimeLogging(client));
    }

    if (!runtimeLogging.isSetWaitNotifyDebug()) {
      runtimeLogging.setWaitNotifyDebug(getDefaultWaitNotifyDebugRuntimeLogging(client));
    }

    if (!runtimeLogging.isSetDistributedMethodDebug()) {
      runtimeLogging.setDistributedMethodDebug(getDefaultDistributedMethodDebugRuntimeLogging(client));
    }

    if (!runtimeLogging.isSetNewObjectDebug()) {
      runtimeLogging.setNewObjectDebug(getDefaultNewObjectDebugRuntimeLogging(client));
    }

    if (!runtimeLogging.isSetNamedLoaderDebug()) {
      runtimeLogging.setNamedLoaderDebug(getDefaultNamedLoaderDebugRuntimeLogging(client));
    }
  }

  private void checkAndSetRuntimeOutputOptions(Client client, RuntimeOutputOptions runtimeOutputOptions)
      throws XmlException {
    if (!runtimeOutputOptions.isSetAutoLockDetails()) {
      runtimeOutputOptions.setAutoLockDetails(getDefaultAutoLockDetailsRuntimeOutputOption(client));
    }

    if (!runtimeOutputOptions.isSetCaller()) {
      runtimeOutputOptions.setCaller(getDefaultCallerRuntimeOutputOption(client));
    }

    if (!runtimeOutputOptions.isSetFullStack()) {
      runtimeOutputOptions.setFullStack(getDefaultFullStackRuntimeOutputOption(client));
    }
  }

  private int getDefaultFaultCount(Client client) throws XmlException {
    return ((XmlInteger) this.defaultValueProvider.defaultFor(client.schemaType(), "dso/fault-count"))
        .getBigIntegerValue().intValue();
  }

  private boolean getDefaultHierarchyInstrumentationLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/instrumentation-logging/hierarchy"))
        .getBooleanValue();
  }

  private boolean getDefaultLocksInstrumentationLoggings(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/instrumentation-logging/locks"))
        .getBooleanValue();
  }

  private boolean getDefaultTransientRootInstrumentationLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/instrumentation-logging/transient-root"))
        .getBooleanValue();
  }

  private boolean getDefaultRootsInstrumentationLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/instrumentation-logging/roots"))
        .getBooleanValue();
  }

  private boolean getDefaultDistributedMethodInstrumentationLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider
        .defaultFor(client.schemaType(), "dso/debugging/instrumentation-logging/distributed-methods"))
        .getBooleanValue();
  }

  private boolean getDefaultClassInstrumentationLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/instrumentation-logging/class"))
        .getBooleanValue();
  }

  private boolean getDefaultAutoLockDetailsRuntimeOutputOption(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-output-options/auto-lock-details"))
        .getBooleanValue();
  }

  private boolean getDefaultCallerRuntimeOutputOption(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-output-options/caller"))
        .getBooleanValue();
  }

  private boolean getDefaultFullStackRuntimeOutputOption(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-output-options/full-stack"))
        .getBooleanValue();
  }

  private boolean getDefaultNonPortableDumpRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/non-portable-dump"))
        .getBooleanValue();
  }

  private boolean getDefaultLockDebugRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/lock-debug"))
        .getBooleanValue();
  }

  private boolean getDefaultFieldChangeDebugRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/field-change-debug"))
        .getBooleanValue();
  }

  private boolean getDefaultWaitNotifyDebugRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/wait-notify-debug"))
        .getBooleanValue();
  }

  private boolean getDefaultDistributedMethodDebugRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/distributed-method-debug"))
        .getBooleanValue();
  }

  private boolean getDefaultNewObjectDebugRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/new-object-debug"))
        .getBooleanValue();
  }

  private boolean getDefaultNamedLoaderDebugRuntimeLogging(Client client) throws XmlException {
    return ((XmlBoolean) this.defaultValueProvider.defaultFor(client.schemaType(),
                                                              "dso/debugging/runtime-logging/named-loader-debug"))
        .getBooleanValue();
  }

  public File directoryConfigurationLoadedFrom() {
    return directoryLoadedFrom;
  }

  public boolean loadedFromTrustedSource() {
    return (baseConfigLoadedFromTrustedSource && (this.configurationSpec.shouldOverrideServerTopology() ? serverOverrideConfigLoadedFromTrustedSource
        : true));
  }

  public String rawConfigText() {
    return rawConfigText;
  }

  public String describeSources() {
    return "The configuration specified by '"
           + baseConfigDescription
           + "'"
           + (this.serverOverrideConfigDescription == null ? "" : " and '" + this.serverOverrideConfigDescription + "'");
  }
}
