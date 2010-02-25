/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.sleepycat.je.Database;
import com.tc.objectserver.persistence.DBEnvironment;
import com.tc.objectserver.persistence.TCBytesBytesDatabase;
import com.tc.objectserver.persistence.TCIntToBytesDatabase;
import com.tc.objectserver.persistence.TCLongDatabase;
import com.tc.objectserver.persistence.TCLongToStringDatabase;
import com.tc.objectserver.persistence.TCObjectDatabase;
import com.tc.objectserver.persistence.TCRootDatabase;
import com.tc.objectserver.persistence.sleepycat.DatabaseOpenResult;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DerbyDBEnvironment implements DBEnvironment {
  public void newLongBlobDatabse(String name) {
    //
  }

  public static boolean tableExists(Connection connection, String table) throws SQLException {
    DatabaseMetaData dbmd = connection.getMetaData();

    String[] types = { "TABLE" };
    ResultSet resultSet = dbmd.getTables(null, null, "%", types);
    while (resultSet.next()) {
      String tableName = resultSet.getString(3);
      if (tableName.equalsIgnoreCase(table)) {
        resultSet.close();
        connection.commit();
        return true;
      }
    }
    return false;
  }

  public synchronized DatabaseOpenResult open() throws TCDatabaseException {
    // TODO
    return null;
  }

  public synchronized void close() throws TCDatabaseException {
    // TODO
  }

  public synchronized boolean isOpen() {
    // TODO
    return false;
  }

  public File getEnvironmentHome() {
    // TODO
    return null;
  }

  public static final String getClusterStateStoreName() {
    // TODO
    return null;
  }

  // TODO: See what has to be done for getStats and getEnvironment

  // TODO: See also getClassCatalogWrapper

  public synchronized TCObjectDatabase getObjectDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public synchronized TCBytesBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public synchronized TCBytesBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public synchronized TCBytesBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public synchronized TCRootDatabase getRootDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public TCLongDatabase getClientStateDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public TCBytesBytesDatabase getTransactionDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public Database getGlobalSequenceDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public Database getMapsDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public Database getClusterStateStoreDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }
}
