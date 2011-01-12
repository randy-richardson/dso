/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.mchange.v2.c3p0.ConnectionCustomizer;

import java.sql.Connection;

public class TCDerbyConnectionListener implements ConnectionCustomizer {

  public void onAcquire(Connection arg0, String arg1) throws Exception {
    System.err.println("onAcquire Connection: " + arg0 + " acquired: " + arg1);
  }

  public void onCheckIn(Connection arg0, String arg1) throws Exception {
    System.err.println("onCheckIn Connection: " + arg0 + " acquired: " + arg1);

  }

  public void onCheckOut(Connection arg0, String arg1) throws Exception {
    System.err.println("onCheckOut Connection: " + arg0 + " acquired: " + arg1);

  }

  public void onDestroy(Connection arg0, String arg1) throws Exception {
    System.err.println("onDestroy Connection: " + arg0 + " acquired: " + arg1);

  }

}
