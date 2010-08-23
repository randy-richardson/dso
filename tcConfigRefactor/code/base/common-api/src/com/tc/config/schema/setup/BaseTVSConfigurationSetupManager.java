/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.context.StandardConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.BeanRepository;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.repository.StandardApplicationsRepository;
import com.tc.config.schema.repository.StandardBeanRepository;
import com.tc.config.schema.utils.XmlObjectComparator;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewDSOApplicationConfigObject;
import com.tc.util.Assert;
import com.terracottatech.config.Application;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoApplication;
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
import com.terracottatech.config.PersistenceMode;
import com.terracottatech.config.RuntimeLogging;
import com.terracottatech.config.RuntimeOutputOptions;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;
import com.terracottatech.config.TcProperties;
import com.terracottatech.config.UpdateCheck;
import com.terracottatech.config.HaMode.Enum;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A base class for all TVS configuration setup managers.
 */
public class BaseTVSConfigurationSetupManager {
  public static final short                       DEFAULT_JMXPORT_OFFSET_FROM_DSOPORT   = 10;
  public static final short                       DEFAULT_GROUPPORT_OFFSET_FROM_DSOPORT = 20;
  public static final int                         MIN_PORTNUMBER                        = 0x0FFF;
  public static final int                         MAX_PORTNUMBER                        = 0xFFFF;

  private final ConfigurationCreator              configurationCreator;
  private final MutableBeanRepository             clientBeanRepository;
  private final MutableBeanRepository             serversBeanRepository;
  private final MutableBeanRepository             systemBeanRepository;
  private final MutableBeanRepository             tcPropertiesRepository;
  private final ApplicationsRepository            applicationsRepository;

  protected final DefaultValueProvider            defaultValueProvider;
  private final XmlObjectComparator               xmlObjectComparator;
  private final IllegalConfigurationChangeHandler illegalConfigurationChangeHandler;

  private final Map                               dsoApplicationConfigs;
  private final Map                               springApplicationConfigs;

  public BaseTVSConfigurationSetupManager(ConfigurationCreator configurationCreator,
                                          DefaultValueProvider defaultValueProvider,
                                          XmlObjectComparator xmlObjectComparator,
                                          IllegalConfigurationChangeHandler illegalConfigurationChangeHandler) {
    Assert.assertNotNull(configurationCreator);
    Assert.assertNotNull(defaultValueProvider);
    Assert.assertNotNull(xmlObjectComparator);
    Assert.assertNotNull(illegalConfigurationChangeHandler);

    this.configurationCreator = configurationCreator;
    this.systemBeanRepository = new StandardBeanRepository(System.class);
    this.clientBeanRepository = new StandardBeanRepository(Client.class);
    this.serversBeanRepository = new StandardBeanRepository(Servers.class);
    this.tcPropertiesRepository = new StandardBeanRepository(TcProperties.class);
    this.applicationsRepository = new StandardApplicationsRepository();

    this.defaultValueProvider = defaultValueProvider;
    this.xmlObjectComparator = xmlObjectComparator;
    this.illegalConfigurationChangeHandler = illegalConfigurationChangeHandler;

    this.dsoApplicationConfigs = new HashMap();
    this.springApplicationConfigs = new HashMap();
  }

  protected final MutableBeanRepository clientBeanRepository() {
    return this.clientBeanRepository;
  }

  public final MutableBeanRepository serversBeanRepository() {
    return this.serversBeanRepository;
  }

  protected final MutableBeanRepository systemBeanRepository() {
    return this.systemBeanRepository;
  }

  protected final MutableBeanRepository tcPropertiesRepository() {
    return this.tcPropertiesRepository;
  }

  protected final ApplicationsRepository applicationsRepository() {
    return this.applicationsRepository;
  }

  protected final XmlObjectComparator xmlObjectComparator() {
    return this.xmlObjectComparator;
  }

  protected final ConfigurationCreator configurationCreator() {
    return this.configurationCreator;
  }

  protected final void runConfigurationCreator() throws ConfigurationSetupException {
    this.configurationCreator.createConfigurationIntoRepositories(clientBeanRepository, serversBeanRepository,
                                                                  systemBeanRepository, tcPropertiesRepository,
                                                                  applicationsRepository);
    initializeDefaults();
  }

  private void initializeDefaults() throws ConfigurationSetupException {
    initializeServer();
    initializeClient();
  }

  private void initializeServer() throws ConfigurationSetupException {
    initializeServerDefaults();
    initializeMirrorGroups();
    initializeUpdateCheck();
  }

  private void initializeServerDefaults() {
    Servers servers = (Servers) serversBeanRepository.bean();
    Server[] serverArray = servers.getServerArray();
    for (int i = 0; i < serverArray.length; i++) {
      initializeServerDefaults(serverArray[i]);
    }
  }

  private void initializeServerDefaults(Server server) {
    initializeDsoPort(server);
    initializeJmxPort(server);
    initializeL2GroupPort(server);
    initializeDataDiretcory(server);
    initializeLogsDiretcory(server);
    initializeDataBackupDiretcory(server);
    initializeStatisticsDiretcory(server);
    initializeDso(server);
  }

  private void initializeDsoPort(Server server) {
    XmlObject[] dsoPorts = server.selectPath("dso-port");
    Assert.assertTrue(dsoPorts.length <= 1);
    if (!server.isSetDsoPort()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class,
                                                                   new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator
          .directoryConfigurationLoadedFrom());
      int defaultdsoPort = configContext.intItem("dso-port").getInt();
      BindPort dsoPort = server.addNewDsoPort();
      dsoPort.setIntValue(defaultdsoPort);
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

  private void initializeDataDiretcory(Server server) {
    if (!server.isSetData()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class,
                                                                   new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator
          .directoryConfigurationLoadedFrom());
      server.setData(configContext.configRelativeSubstitutedFileItem("data").getFile().getAbsolutePath());
    } else {
      server.setData(ParameterSubstituter.substitute(server.getData()));
    }
  }

  private void initializeLogsDiretcory(Server server) {
    if (!server.isSetLogs()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class,
                                                                   new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator
          .directoryConfigurationLoadedFrom());
      server.setLogs(configContext.configRelativeSubstitutedFileItem("logs").getFile().getAbsolutePath());
    } else {
      server.setLogs(ParameterSubstituter.substitute(server.getLogs()));
    }
  }

  private void initializeDataBackupDiretcory(Server server) {
    if (!server.isSetDataBackup()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class,
                                                                   new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator
          .directoryConfigurationLoadedFrom());
      server.setDataBackup(configContext.configRelativeSubstitutedFileItem("data-backup").getFile().getAbsolutePath());
    } else {
      server.setDataBackup(ParameterSubstituter.substitute(server.getDataBackup()));
    }
  }

  private void initializeStatisticsDiretcory(Server server) {
    if (!server.isSetStatistics()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class,
                                                                   new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator
          .directoryConfigurationLoadedFrom());
      server.setStatistics(configContext.configRelativeSubstitutedFileItem("statistics").getFile().getAbsolutePath());
    } else {
      server.setStatistics(ParameterSubstituter.substitute(server.getStatistics()));
    }
  }

  private void initializeDso(Server server) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class,
                                                                 new BeanFetcher(server));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    if (!server.isSetDso()) {
      DsoServerData dso = server.addNewDso();
      dso.addNewPersistence().setMode(getDefaultPersistence(configContext));
      dso.setClientReconnectWindow(getDefaultReconnectWindow(configContext));
      setDefaultGarbageCollection(configContext, dso.addNewGarbageCollection());
    } else {
      DsoServerData dso = server.getDso();

      if (!dso.isSetPersistence()) {
        dso.addNewPersistence().setMode(getDefaultPersistence(configContext));
      }

      if (!dso.isSetClientReconnectWindow()) {
        dso.setClientReconnectWindow(getDefaultReconnectWindow(configContext));
      }

      if (!dso.isSetGarbageCollection()) {
        setDefaultGarbageCollection(configContext, dso.addNewGarbageCollection());
      } else {
        GarbageCollection gc = dso.getGarbageCollection();
        if (!gc.isSetEnabled()) {
          gc.setEnabled(getDefaultGarbageCollectionEnabled(configContext));
        }

        if (!gc.isSetVerbose()) {
          gc.setVerbose(getDefaultGarbageCollectionVerbose(configContext));
        }

        if (!gc.isSetInterval()) {
          gc.setInterval(getDefaultGarbageCollectionInterval(configContext));
        }
      }
    }
  }

  private void setDefaultGarbageCollection(ConfigContext configContext, GarbageCollection gc) {
    gc.setEnabled(getDefaultGarbageCollectionEnabled(configContext));
    gc.setVerbose(getDefaultGarbageCollectionVerbose(configContext));
    gc.setInterval(getDefaultGarbageCollectionInterval(configContext));
  }

  private boolean getDefaultGarbageCollectionEnabled(ConfigContext configContext) {
    return configContext.booleanItem("dso/garbage-collection/enabled").getBoolean();
  }

  private boolean getDefaultGarbageCollectionVerbose(ConfigContext configContext) {
    return configContext.booleanItem("dso/garbage-collection/verbose").getBoolean();
  }

  private int getDefaultGarbageCollectionInterval(ConfigContext configContext) {
    return configContext.intItem("dso/garbage-collection/interval").getInt();
  }

  private PersistenceMode.Enum getDefaultPersistence(ConfigContext configContext) {
    ConfigItem persistenceMode = new XPathBasedConfigItem(configContext, "dso/persistence/mode") {
      @Override
      protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
        if (xmlObject == null) return null;
        Assert.assertTrue((((PersistenceMode) xmlObject).enumValue() == PersistenceMode.TEMPORARY_SWAP_ONLY)
                          || (((PersistenceMode) xmlObject).enumValue() == PersistenceMode.PERMANENT_STORE));
        return ((PersistenceMode) xmlObject).enumValue();
      }
    };
    return (PersistenceMode.Enum) persistenceMode.getObject();
  }

  private int getDefaultReconnectWindow(ConfigContext configContext) {
    return configContext.intItem("dso/client-reconnect-window").getInt();
  }

  private void initializeMirrorGroups() throws ConfigurationSetupException {
    Servers servers = (Servers) serversBeanRepository.bean();
    if (!servers.isSetMirrorGroups()) {
      createDefaultServerMirrorGroups();
    } else {
      MirrorGroup[] mirrorGroups = servers.getMirrorGroups().getMirrorGroupArray();
      for (MirrorGroup mirrorGroup : mirrorGroups) {
        if (!mirrorGroup.isSetHa()) {
          Ha ha;
          try {
            ha = servers.isSetHa() ? servers.getHa() : getDefaultCommonHa();
          } catch (XmlException e) {
            throw new ConfigurationSetupException(e);
          }
          mirrorGroup.setHa(ha);
        }
      }
    }
  }

  private void createDefaultServerMirrorGroups() throws ConfigurationSetupException {
    Servers servers = (Servers) serversBeanRepository.bean();
    Ha ha;
    try {
      ha = servers.isSetHa() ? servers.getHa() : getDefaultCommonHa();
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

  private void initializeUpdateCheck() throws ConfigurationSetupException {
    Servers servers = (Servers) serversBeanRepository.bean();
    if (!servers.isSetUpdateCheck()) {
      try {
        servers.setUpdateCheck(getDefaultUpdateCheck());
      } catch (XmlException e) {
        throw new ConfigurationSetupException(e);
      }
    }
  }

  private void initializeClient() {
    initializeLogsDirectory();
    initializeModules();
    initiailizeDsoClient();
  }

  private void initializeLogsDirectory() {
    Client client = (Client) clientBeanRepository.bean();
    if (client != null && !client.isSetLogs()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                   new BeanFetcher(client));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator
          .directoryConfigurationLoadedFrom());
      client.setLogs(configContext.substitutedFileItem("logs").getFile().getAbsolutePath());
    }
  }

  private void initializeModules() {
    Client client = (Client) clientBeanRepository.bean();
    if (client != null && client.isSetModules()) {
      Modules modules = client.getModules();
      for (int i = 0; i < modules.sizeOfRepositoryArray(); i++) {
        String location = modules.getRepositoryArray(i);
        modules.setRepositoryArray(i, ParameterSubstituter.substitute(location));
      }
    }
  }

  private void initiailizeDsoClient() {
    Client client = (Client) clientBeanRepository.bean();
    if (client != null) {
      if (!client.isSetDso()) {
        DsoClientData dsoClientData = client.addNewDso();
        dsoClientData.setFaultCount(getDefaultFaultCount());

        DsoClientDebugging debugging = dsoClientData.addNewDebugging();
        addDefaultInstrumentationLogging(debugging);
        addDefaultRuntimeLogging(debugging);
        addDefaultRuntimeOutputOptions(debugging);
      } else {
        DsoClientData dsoClientData = client.getDso();
        if (!dsoClientData.isSetFaultCount()) {
          dsoClientData.setFaultCount(getDefaultFaultCount());
        }

        if (!dsoClientData.isSetDebugging()) {
          DsoClientDebugging debugging = dsoClientData.addNewDebugging();
          addDefaultInstrumentationLogging(debugging);
          addDefaultRuntimeLogging(debugging);
        } else {
          DsoClientDebugging debugging = dsoClientData.getDebugging();
          if (!debugging.isSetInstrumentationLogging()) {
            addDefaultInstrumentationLogging(debugging);
          }else{
            checkAndSetInstrumentationLogging(debugging.getInstrumentationLogging());
          }
          
          if(!debugging.isSetRuntimeLogging()){
            addDefaultRuntimeLogging(debugging);
          }else{
            checkAndSetRuntimeLogging(debugging.getRuntimeLogging());
          }
          
          if(!debugging.isSetRuntimeOutputOptions()){
            addDefaultRuntimeOutputOptions(debugging);
          }else{
            checkAndSetRuntimeOutputOptions(debugging.getRuntimeOutputOptions());
          }
        }
      }
    }
  }
  
  private void addDefaultInstrumentationLogging(DsoClientDebugging debugging) {
    checkAndSetInstrumentationLogging(debugging.addNewInstrumentationLogging());
  }

  private void addDefaultRuntimeLogging(DsoClientDebugging debugging) {
    checkAndSetRuntimeLogging(debugging.addNewRuntimeLogging());
  }
  
  private void addDefaultRuntimeOutputOptions(DsoClientDebugging debugging) {
    checkAndSetRuntimeOutputOptions(debugging.addNewRuntimeOutputOptions());
  }

  private void checkAndSetInstrumentationLogging(InstrumentationLogging instrumentationLogging) {
    if(!instrumentationLogging.isSetClass1()){
      instrumentationLogging.setClass1(getDefaultClassInstrumentationLogging());
    }
    
    if(!instrumentationLogging.isSetHierarchy()){
      instrumentationLogging.setHierarchy(getDefaultHierarchyInstrumentationLogging());
    }
    
    if(!instrumentationLogging.isSetLocks()){
      instrumentationLogging.setLocks(getDefaultLocksInstrumentationLoggings());
    }
    
    if(!instrumentationLogging.isSetTransientRoot()){
      instrumentationLogging.setTransientRoot(getDefaultTransientRootInstrumentationLogging());
    }
    
    if(!instrumentationLogging.isSetRoots()){
      instrumentationLogging.setRoots(getDefaultRootsInstrumentationLogging());
    }
    
    if(!instrumentationLogging.isSetDistributedMethods()){
      instrumentationLogging.setDistributedMethods(getDefaultDistributedMethodInstrumentationLogging());
    }
  }
  
  private void checkAndSetRuntimeLogging(RuntimeLogging runtimeLogging) {
    if(!runtimeLogging.isSetNonPortableDump()){
      runtimeLogging.setNonPortableDump(getDefaultNonPortableDumpRuntimeLogging());
    }
    
    if(!runtimeLogging.isSetLockDebug()){
      runtimeLogging.setLockDebug(getDefaultLockDebugRuntimeLogging());
    }
    
    if(!runtimeLogging.isSetFieldChangeDebug()){
      runtimeLogging.setFieldChangeDebug(getDefaultFieldChangeDebugRuntimeLogging());
    }
    
    if(!runtimeLogging.isSetWaitNotifyDebug()){
      runtimeLogging.setWaitNotifyDebug(getDefaultWaitNotifyDebugRuntimeLogging());
    }
    
    if(!runtimeLogging.isSetDistributedMethodDebug()){
      runtimeLogging.setDistributedMethodDebug(getDefaultDistributedMethodDebugRuntimeLogging());
    }
    
    if(!runtimeLogging.isSetNewObjectDebug()){
      runtimeLogging.setNewObjectDebug(getDefaultNewObjectDebugRuntimeLogging());
    }
    
    if(!runtimeLogging.isSetNamedLoaderDebug()){
      runtimeLogging.setNamedLoaderDebug(getDefaultNamedLoaderDebugRuntimeLogging());
    }
  }
  
  private void checkAndSetRuntimeOutputOptions(RuntimeOutputOptions runtimeOutputOptions) {
    if(!runtimeOutputOptions.isSetAutoLockDetails()){
      runtimeOutputOptions.setAutoLockDetails(getDefaultAutoLockDetailsRuntimeOutputOption());
    }
    
    if(!runtimeOutputOptions.isSetCaller()){
      runtimeOutputOptions.setCaller(getDefaultCallerRuntimeOutputOption());
    }
    
    if(!runtimeOutputOptions.isSetFullStack()){
      runtimeOutputOptions.setFullStack(getDefaultFullStackRuntimeOutputOption());
    }
  }
  
  private int getDefaultFaultCount() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.intItem("dso/fault-count").getInt();
  }
  
  private boolean getDefaultHierarchyInstrumentationLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/instrumentation-logging/hierarchy").getBoolean();
  }

  private boolean getDefaultLocksInstrumentationLoggings() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/instrumentation-logging/locks").getBoolean();
  }

  private boolean getDefaultTransientRootInstrumentationLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/instrumentation-logging/transient-root").getBoolean();
  }

  private boolean getDefaultRootsInstrumentationLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/instrumentation-logging/roots").getBoolean();
  }

  private boolean getDefaultDistributedMethodInstrumentationLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/instrumentation-logging/distributed-methods").getBoolean();
  }

  private boolean getDefaultClassInstrumentationLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/instrumentation-logging/class").getBoolean();
  }
  
  private boolean getDefaultAutoLockDetailsRuntimeOutputOption() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-output-options/auto-lock-details").getBoolean();
  }

  private boolean getDefaultCallerRuntimeOutputOption() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-output-options/caller").getBoolean();
  }

  private boolean getDefaultFullStackRuntimeOutputOption() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-output-options/full-stack").getBoolean();
  }

  private boolean getDefaultNonPortableDumpRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/non-portable-dump").getBoolean();
  }

  private boolean getDefaultLockDebugRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/lock-debug").getBoolean();
  }

  private boolean getDefaultFieldChangeDebugRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/field-change-debug").getBoolean();
  }

  private boolean getDefaultWaitNotifyDebugRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/wait-notify-debug").getBoolean();
  }

  private boolean getDefaultDistributedMethodDebugRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/distributed-method-debug").getBoolean();
  }

  private boolean getDefaultNewObjectDebugRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/new-object-debug").getBoolean();
  }

  private boolean getDefaultNamedLoaderDebugRuntimeLogging() {
    ChildBeanRepository beanRepository = new ChildBeanRepository(clientBeanRepository(), Client.class,
                                                                 new BeanFetcher(clientBeanRepository.bean()));
    ConfigContext configContext = createContext(beanRepository, this.configurationCreator
        .directoryConfigurationLoadedFrom());
    return configContext.booleanItem("dso/debugging/runtime-logging/named-loader-debug").getBoolean();
  }

  private class BeanFetcher implements ChildBeanFetcher {
    private final XmlObject xmlObject;

    public BeanFetcher(XmlObject xmlObject) {
      this.xmlObject = xmlObject;
    }

    public XmlObject getChild(XmlObject parent) {
      return this.xmlObject;
    }
  }

  public String[] applicationNames() {
    Set names = new HashSet();

    names.addAll(this.dsoApplicationConfigs.keySet());
    names.addAll(this.springApplicationConfigs.keySet());

    return (String[]) names.toArray(new String[names.size()]);
  }

  public final ConfigContext createContext(BeanRepository beanRepository, File configFilePath) {
    Assert.assertNotNull(beanRepository);
    return new StandardConfigContext(beanRepository, this.defaultValueProvider, this.illegalConfigurationChangeHandler,
                                     configFilePath);
  }

  public synchronized NewDSOApplicationConfig dsoApplicationConfigFor(String applicationName) {
    // When we support multiple applications, just take this assertion out.
    Assert.eval(applicationName.equals(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME));

    NewDSOApplicationConfig out = (NewDSOApplicationConfig) this.dsoApplicationConfigs.get(applicationName);
    if (out == null) {
      out = createNewDSOApplicationConfig(applicationName);
      this.dsoApplicationConfigs.put(applicationName, out);
    }

    return out;
  }

  protected NewDSOApplicationConfig createNewDSOApplicationConfig(String applicationName) {
    return new NewDSOApplicationConfigObject(createContext(new ChildBeanRepository(this.applicationsRepository
        .repositoryFor(applicationName), DsoApplication.class, new ChildBeanFetcher() {
      public XmlObject getChild(XmlObject parent) {
        return ((Application) parent).getDso();
      }
    }), null));
  }

  private Ha getDefaultCommonHa() throws XmlException {
    final int defaultElectionTime = ((XmlInteger) defaultValueProvider.defaultFor(serversBeanRepository
        .rootBeanSchemaType(), "ha/networked-active-passive/election-time")).getBigIntegerValue().intValue();
    final String defaultHaModeString = ((XmlString) defaultValueProvider.defaultFor(serversBeanRepository
        .rootBeanSchemaType(), "ha/mode")).getStringValue();
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

  private UpdateCheck getDefaultUpdateCheck() throws XmlException {
    final int defaultPeriodDays = ((XmlInteger) defaultValueProvider.defaultFor(serversBeanRepository()
        .rootBeanSchemaType(), "update-check/period-days")).getBigIntegerValue().intValue();
    final boolean defaultEnabled = ((XmlBoolean) defaultValueProvider.defaultFor(serversBeanRepository()
        .rootBeanSchemaType(), "update-check/enabled")).getBooleanValue();
    UpdateCheck uc = UpdateCheck.Factory.newInstance();
    uc.setEnabled(defaultEnabled);
    uc.setPeriodDays(defaultPeriodDays);
    return uc;
  }
}
