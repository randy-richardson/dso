/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.DBEnvironment;
import com.tc.objectserver.persistence.TCBytesBytesDatabase;
import com.tc.objectserver.persistence.TCIntToBytesDatabase;
import com.tc.objectserver.persistence.TCLongDatabase;
import com.tc.objectserver.persistence.TCLongToStringDatabase;
import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.TCObjectDatabase;
import com.tc.objectserver.persistence.TCRootDatabase;
import com.tc.objectserver.persistence.TCStringToStringDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.DatabaseOpenResult;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.util.sequence.MutableSequence;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DerbyDBEnvironment implements DBEnvironment {
  private final Map  tables = new HashMap();
  private Connection connection;

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
    createTablesIfRequired();
    return null;
  }

  private void createTablesIfRequired() throws TCDatabaseException {
    newObjectDB();
    newRootDB();
    newBytesToBlobDB(OBJECT_OID_STORE_DB_NAME);
    newBytesToBlobDB(MAPS_OID_STORE_DB_NAME);
    newBytesToBlobDB(OID_STORE_LOG_DB_NAME);
    newLongDB(CLIENT_STATE_DB_NAME);
    newBytesToBlobDB(TRANSACTION_DB_NAME);
    newIntToBytesDB(CLASS_DB_NAME);
    newLongToStringDB(STRING_INDEX_DB_NAME);
    newStringToStringDB(CLUSTER_STATE_STORE);
  }

  private void newObjectDB() throws TCDatabaseException {
    TCObjectDatabase db = new DerbyTCObjectDatabase(OBJECT_DB_NAME, connection);
    tables.put(OBJECT_DB_NAME, db);
  }

  private void newRootDB() throws TCDatabaseException {
    TCRootDatabase db = new DerbyTCRootDatabase(ROOT_DB_NAME, connection);
    tables.put(ROOT_DB_NAME, db);
  }

  private void newBytesToBlobDB(String tableName) throws TCDatabaseException {
    TCBytesBytesDatabase db = new DerbyTCBytesToBlobDB(tableName, connection);
    tables.put(tableName, db);
  }

  private void newLongDB(String tableName) throws TCDatabaseException {
    TCLongDatabase db = new DerbyTCLongDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newIntToBytesDB(String tableName) throws TCDatabaseException {
    TCIntToBytesDatabase db = new DerbyTCIntToBytesDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newLongToStringDB(String tableName) throws TCDatabaseException {
    TCLongToStringDatabase db = new DerbyTCLongToStringDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newStringToStringDB(String tableName) throws TCDatabaseException {
    TCStringToStringDatabase db = new DerbyTCStringToStringDatabase(tableName, connection);
    tables.put(tableName, db);
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

  public synchronized TCObjectDatabase getObjectDatabase() {
    return (DerbyTCObjectDatabase) tables.get(OBJECT_DB_NAME);
  }

  public synchronized TCBytesBytesDatabase getObjectOidStoreDatabase() {
    return (DerbyTCBytesToBlobDB) tables.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesBytesDatabase getMapsOidStoreDatabase() {
    return (DerbyTCBytesToBlobDB) tables.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesBytesDatabase getOidStoreLogDatabase() {
    return (DerbyTCBytesToBlobDB) tables.get(OID_STORE_LOG_DB_NAME);
  }

  public synchronized TCRootDatabase getRootDatabase() {
    return (DerbyTCRootDatabase) tables.get(ROOT_DB_NAME);
  }

  public TCLongDatabase getClientStateDatabase() {
    return (DerbyTCLongDatabase) tables.get(CLIENT_STATE_DB_NAME);
  }

  public TCBytesBytesDatabase getTransactionDatabase() {
    return (DerbyTCBytesToBlobDB) tables.get(TRANSACTION_DB_NAME);
  }

  public TCIntToBytesDatabase getClassDatabase() {
    return (DerbyTCIntToBytesDatabase) tables.get(CLASS_DB_NAME);
  }

  public TCMapsDatabase getMapsDatabase() throws TCDatabaseException {
    // TODO
    return null;
  }

  public TCLongToStringDatabase getStringIndexDatabase() {
    return (DerbyTCLongToStringDatabase) tables.get(STRING_INDEX_DB_NAME);
  }

  public TCStringToStringDatabase getClusterStateStoreDatabase() {
    return (DerbyTCStringToStringDatabase) tables.get(CLUSTER_STATE_STORE);
  }

  public MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger logger, String sequenceID,
                                     int startValue) {
    // TODO
    return null;
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    // TODO
    return null;
  }

  public boolean isParanoidMode() {
    // TODO
    return false;
  }
}
