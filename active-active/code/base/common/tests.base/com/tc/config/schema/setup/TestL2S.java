/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.exception.ImplementMe;
import com.terracottatech.config.ActiveServerGroups;
import com.terracottatech.config.Ha;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

public class TestL2S extends TestXmlObject implements Servers {

  private Server[] servers;

  public TestL2S(Server[] servers) {
    this.servers = servers;
  }

  public TestL2S() {
    this(null);
  }

  public void setServers(Server[] servers) {
    this.servers = servers;
  }

  public Server[] getServerArray() {
    return this.servers;
  }

  public Server getServerArray(int arg0) {
    return this.servers[arg0];
  }

  public int sizeOfServerArray() {
    return this.servers.length;
  }

  public void setServerArray(Server[] arg0) {
    throw new ImplementMe();
  }

  public void setServerArray(int arg0, Server arg1) {
    throw new ImplementMe();
  }

  public Server insertNewServer(int arg0) {
    throw new ImplementMe();
  }

  public Server addNewServer() {
    throw new ImplementMe();
  }

  public void removeServer(int arg0) {
    throw new ImplementMe();
  }

  public Ha addNewHa() {
    throw new ImplementMe();
  }

  public ActiveServerGroups addNewActiveServerGroups() {
    throw new ImplementMe();
  }

  public ActiveServerGroups[] getActiveServerGroupsArray() {
    throw new ImplementMe();
  }

  public ActiveServerGroups getActiveServerGroupsArray(int arg0) {
    throw new ImplementMe();
  }

  public Ha[] getHaArray() {
    throw new ImplementMe();
  }

  public Ha getHaArray(int arg0) {
    throw new ImplementMe();
  }

  public ActiveServerGroups insertNewActiveServerGroups(int arg0) {
    throw new ImplementMe();
  }

  public Ha insertNewHa(int arg0) {
    throw new ImplementMe();
  }

  public void removeActiveServerGroups(int arg0) {
    throw new ImplementMe();
  }

  public void removeHa(int arg0) {
    throw new ImplementMe();
  }

  public void setActiveServerGroupsArray(ActiveServerGroups[] arg0) {
    throw new ImplementMe();
  }

  public void setActiveServerGroupsArray(int arg0, ActiveServerGroups arg1) {
    throw new ImplementMe();
  }

  public void setHaArray(Ha[] arg0) {
    throw new ImplementMe();
  }

  public void setHaArray(int arg0, Ha arg1) {
    throw new ImplementMe();
  }

  public int sizeOfActiveServerGroupsArray() {
    throw new ImplementMe();
  }

  public int sizeOfHaArray() {
    throw new ImplementMe();
  }

}
