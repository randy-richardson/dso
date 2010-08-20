/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.context.StandardConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
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
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.System;
import com.terracottatech.config.TcProperties;

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

  private final ConfigurationCreator configurationCreator;
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

  public BaseTVSConfigurationSetupManager(ConfigurationCreator configurationCreator, DefaultValueProvider defaultValueProvider,
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
  
  protected final ConfigurationCreator configurationCreator(){
    return this.configurationCreator;
  }

  protected final void runConfigurationCreator()
      throws ConfigurationSetupException {
    this.configurationCreator.createConfigurationIntoRepositories(clientBeanRepository, serversBeanRepository,
                                                             systemBeanRepository, tcPropertiesRepository,
                                                             applicationsRepository);
    initializeDefaults();
  }

  private void initializeDefaults() {
    initializeServerDefaults();
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
  }

  private void initializeDsoPort(Server server) {
    XmlObject[] dsoPorts = server.selectPath("dso-port");
    Assert.assertTrue(dsoPorts.length <= 1);
    if (!server.isSetDsoPort()) {
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator.directoryConfigurationLoadedFrom());
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
    if(!server.isSetData()){
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator.directoryConfigurationLoadedFrom());
      server.setData(configContext.configRelativeSubstitutedFileItem("data").getFile().getAbsolutePath());
    }else{
      server.setData(ParameterSubstituter.substitute(server.getData()));
    }
  }
  
  private void initializeLogsDiretcory(Server server) {
    if(!server.isSetLogs()){
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator.directoryConfigurationLoadedFrom());
      server.setLogs(configContext.configRelativeSubstitutedFileItem("logs").getFile().getAbsolutePath());
    }else{
      server.setLogs(ParameterSubstituter.substitute(server.getLogs()));
    }
  }
  
  private void initializeDataBackupDiretcory(Server server) {
    if(!server.isSetDataBackup()){
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator.directoryConfigurationLoadedFrom());
      server.setDataBackup(configContext.configRelativeSubstitutedFileItem("data-backup").getFile().getAbsolutePath());
    }else{
      server.setDataBackup(ParameterSubstituter.substitute(server.getDataBackup()));
    }
  }
  
  private void initializeStatisticsDiretcory(Server server) {
    if(!server.isSetStatistics()){
      ChildBeanRepository beanRepository = new ChildBeanRepository(serversBeanRepository(), Server.class, new BeanFetcher(server));
      ConfigContext configContext = createContext(beanRepository, this.configurationCreator.directoryConfigurationLoadedFrom());
      server.setStatistics(configContext.configRelativeSubstitutedFileItem("statistics").getFile().getAbsolutePath());
    }else{
      server.setStatistics(ParameterSubstituter.substitute(server.getStatistics()));
    }
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
}
