/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.BaseNewConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.util.Assert;
import com.terracottatech.config.BindPort;
import com.terracottatech.config.Offheap;
import com.terracottatech.config.Server;

/**
 * The standard implementation of {@link NewL2DSOConfig}.
 */
public class NewL2DSOConfigObject extends BaseNewConfigObject implements NewL2DSOConfig {

  private final PersistenceMode persistenceMode;
  private final Offheap         offHeapConfig;
  private final boolean         garbageCollectionEnabled;
  private final boolean         garbageCollectionVerbose;
  private final int             garbageCollectionInterval;
  private final BindPort        dsoPort;
  private final BindPort        l2GroupPort;
  private final int             clientReconnectWindow;
  private final String          host;
  private final String          serverName;
  private final String          bind;

  public NewL2DSOConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(Server.class);
    Server server = (Server) this.context.bean();

    Assert
        .assertTrue((server.getDso().getPersistence().getMode() == com.terracottatech.config.PersistenceMode.PERMANENT_STORE)
                    || (server.getDso().getPersistence().getMode() == com.terracottatech.config.PersistenceMode.TEMPORARY_SWAP_ONLY));
    if (server.getDso().getPersistence().getMode() == com.terracottatech.config.PersistenceMode.PERMANENT_STORE) {
      this.persistenceMode = PersistenceMode.PERMANENT_STORE;
    } else {
      this.persistenceMode = PersistenceMode.TEMPORARY_SWAP_ONLY;
    }

    this.garbageCollectionEnabled = server.getDso().getGarbageCollection().getEnabled();
    this.garbageCollectionVerbose = server.getDso().getGarbageCollection().getVerbose();
    this.garbageCollectionInterval = server.getDso().getGarbageCollection().getInterval();
    this.clientReconnectWindow = server.getDso().getClientReconnectWindow();

    this.bind = server.getBind();
    this.host = server.getHost();
    this.serverName = server.getName();

    this.dsoPort = server.getDsoPort();
    this.l2GroupPort = server.getL2GroupPort();
    this.offHeapConfig = server.getDso().getPersistence().getOffheap();
  }

  public Offheap offHeapConfig() {
    return this.offHeapConfig;
  }

  public BindPort dsoPort() {
    return this.dsoPort;
  }

  public BindPort l2GroupPort() {
    return this.l2GroupPort;
  }

  public String host() {
    return host;
  }

  public String serverName() {
    return this.serverName;
  }

  public PersistenceMode persistenceMode() {
    return this.persistenceMode;
  }

  public boolean garbageCollectionEnabled() {
    return this.garbageCollectionEnabled;
  }

  public boolean garbageCollectionVerbose() {
    return this.garbageCollectionVerbose;
  }

  public int garbageCollectionInterval() {
    return this.garbageCollectionInterval;
  }

  public int clientReconnectWindow() {
    return this.clientReconnectWindow;
  }

  public String bind() {
    return this.bind;
  }
  
  //Used STRICTLY for test

  public void setClientReconnectWindo(int clinetReconnectWindow) {
    Server server = (Server) getBean();
    server.getDso().setClientReconnectWindow(clinetReconnectWindow);
  }

  public void setDsoPort(BindPort dsoPort) {
    Server server = (Server) getBean();
    server.setDsoPort(dsoPort);
  }

  public void setGarbageCollectionInterval(int garbageCollectionInterval) {
    Server server = (Server) getBean();
    server.getDso().getGarbageCollection().setInterval(garbageCollectionInterval);
  }

  public void setGarbageCollectionVerbose(boolean garbageCollectionVerbose) {
    Server server = (Server) getBean();
    server.getDso().getGarbageCollection().setVerbose(garbageCollectionVerbose);
  }

  public void setGrabgeCollectionEnabled(boolean garbageCollectionEnabled) {
    Server server = (Server) getBean();
    server.getDso().getGarbageCollection().setEnabled(garbageCollectionEnabled);
  }

  public void setL2GroupPort(BindPort l2GroupPort) {
    Server server = (Server) getBean();
    server.setL2GroupPort(l2GroupPort);
  }

  public void setOffHeap(Offheap offheap) {
    Server server = (Server) getBean();
    server.getDso().getPersistence().setOffheap(offheap);
  }

  public void setPersistenceMode(PersistenceMode persistenceMode) {
    Server server = (Server) getBean();
    if(persistenceMode == PersistenceMode.PERMANENT_STORE){
      server.getDso().getPersistence().setMode(com.terracottatech.config.PersistenceMode.PERMANENT_STORE);
    }else if(persistenceMode == PersistenceMode.TEMPORARY_SWAP_ONLY){
      server.getDso().getPersistence().setMode(com.terracottatech.config.PersistenceMode.TEMPORARY_SWAP_ONLY);
    }else{
      Assert.failure("Invalid persistence mode: " + persistenceMode);
    }
  }

  public void setBind(String bind) {
    Server server = (Server) getBean();
    server.setBind(bind);
  }

}
