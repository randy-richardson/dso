/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import org.apache.commons.io.FileUtils;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
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
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.DatabaseNotOpenException;
import com.tc.objectserver.persistence.sleepycat.DatabaseOpenResult;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.util.sequence.MutableSequence;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DerbyDBEnvironment implements DBEnvironment {
  public static final String    DRIVER       = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String    PROTOCOL     = "jdbc:derby:";
  public static final String    DB_NAME      = "objectDB";

  private final Map             tables       = new HashMap();
  private final Properties      derbyProps;
  private ComboPooledDataSource cpds;
  private final boolean         isParanoid;
  private final File            envHome;
  private DBEnvironmentStatus   status;
  private DerbyControlDB        controlDB;
  private static final Object   CONTROL_LOCK = new Object();

  public DerbyDBEnvironment(boolean paranoid, File home) throws IOException {
    this(paranoid, home, new Properties());
  }

  public DerbyDBEnvironment(boolean paranoid, File home, Properties props) throws IOException {
    this.isParanoid = paranoid;
    this.envHome = home;
    this.derbyProps = props;
    FileUtils.forceMkdir(this.envHome);
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
    DatabaseOpenResult openResult;
    try {
      openDatabase();

      status = DBEnvironmentStatus.STATUS_OPENING;

      // now open control db
      synchronized (CONTROL_LOCK) {
        controlDB = new DerbyControlDB(CONTROL_DB, createConnection());
        openResult = new DatabaseOpenResult(controlDB.isClean());
        if (!openResult.isClean()) {
          this.status = DBEnvironmentStatus.STATUS_INIT;
          forceClose();
          return openResult;
        }
      }

      if (!this.isParanoid) {
        this.controlDB.setDirty();
      }

      createTablesIfRequired();

    } catch (TCDatabaseException e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    } catch (Error e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    } catch (RuntimeException e) {
      this.status = DBEnvironmentStatus.STATUS_ERROR;
      forceClose();
      throw e;
    }

    status = DBEnvironmentStatus.STATUS_OPEN;
    return openResult;
  }

  public void createDatabase() throws TCDatabaseException {
    // loading the Driver
    try {
      Class.forName(DRIVER).newInstance();
    } catch (ClassNotFoundException cnfe) {
      String message = "Unable to load the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message);
    } catch (InstantiationException ie) {
      String message = "Unable to instantiate the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message);
    } catch (IllegalAccessException iae) {
      String message = "Not allowed to access the JDBC driver " + DRIVER;
      throw new TCDatabaseException(message);
    }

    Properties attributesProps = new Properties();
    attributesProps.put("create", "true");
    Connection conn;
    try {
      conn = DriverManager.getConnection(PROTOCOL + envHome.getAbsolutePath() + File.separator + DB_NAME + ";",
                                         attributesProps);
      conn.setAutoCommit(false);
      conn.close();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  public void openDatabase() throws TCDatabaseException {
    createDatabase();

    try {
      cpds = new ComboPooledDataSource();
      cpds.setDriverClass(DRIVER);
      cpds.setJdbcUrl(PROTOCOL + envHome.getAbsolutePath() + File.separator + DB_NAME + ";");
      cpds.setAutoCommitOnClose(false);
      cpds.setMinPoolSize(50);
      cpds.setAcquireIncrement(5);
      cpds.setMaxPoolSize(300);
      cpds.setProperties(derbyProps);
    } catch (PropertyVetoException e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  protected Connection createConnection() throws TCDatabaseException {
    try {
      Connection conn = cpds.getConnection();
      conn.setAutoCommit(false);
      return conn;
    } catch (SQLException sqlE) {
      throw new TCDatabaseException(sqlE);
    }
  }

  private void createTablesIfRequired() throws TCDatabaseException {
    Connection connection = createConnection();

    newObjectDB(connection);
    newRootDB(connection);
    newBytesToBlobDB(OBJECT_OID_STORE_DB_NAME, connection);
    newBytesToBlobDB(MAPS_OID_STORE_DB_NAME, connection);
    newBytesToBlobDB(OID_STORE_LOG_DB_NAME, connection);
    newLongDB(CLIENT_STATE_DB_NAME, connection);
    newBytesToBlobDB(TRANSACTION_DB_NAME, connection);
    newIntToBytesDB(CLASS_DB_NAME, connection);
    newLongToStringDB(STRING_INDEX_DB_NAME, connection);
    newStringToStringDB(CLUSTER_STATE_STORE, connection);
    newMapsDatabase(connection);

    try {
      DerbyDBSequence.createSequenceTable(connection);
    } catch (SQLException e) {
      try {
        connection.rollback();
        connection.close();
      } catch (SQLException e1) {
        throw new TCDatabaseException(e1);
      }
      throw new TCDatabaseException(e);
    }

    try {
      connection.close();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  private void newObjectDB(Connection connection) throws TCDatabaseException {
    TCObjectDatabase db = new DerbyTCObjectDatabase(OBJECT_DB_NAME, connection);
    tables.put(OBJECT_DB_NAME, db);
  }

  private void newRootDB(Connection connection) throws TCDatabaseException {
    TCRootDatabase db = new DerbyTCRootDatabase(ROOT_DB_NAME, connection);
    tables.put(ROOT_DB_NAME, db);
  }

  private void newBytesToBlobDB(String tableName, Connection connection) throws TCDatabaseException {
    TCBytesBytesDatabase db = new DerbyTCBytesToBlobDB(tableName, connection);
    tables.put(tableName, db);
  }

  private void newLongDB(String tableName, Connection connection) throws TCDatabaseException {
    TCLongDatabase db = new DerbyTCLongDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newIntToBytesDB(String tableName, Connection connection) throws TCDatabaseException {
    TCIntToBytesDatabase db = new DerbyTCIntToBytesDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newLongToStringDB(String tableName, Connection connection) throws TCDatabaseException {
    TCLongToStringDatabase db = new DerbyTCLongToStringDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newStringToStringDB(String tableName, Connection connection) throws TCDatabaseException {
    TCStringToStringDatabase db = new DerbyTCStringToStringDatabase(tableName, connection);
    tables.put(tableName, db);
  }

  private void newMapsDatabase(Connection connection) throws TCDatabaseException {
    TCMapsDatabase db = new DerbyTCMapsDatabase(MAP_DB_NAME, connection);
    tables.put(MAP_DB_NAME, db);
  }

  public synchronized void close() throws TCDatabaseException {
    status = DBEnvironmentStatus.STATUS_CLOSING;

    forceClose();

    status = DBEnvironmentStatus.STATUS_CLOSED;
  }

  private void forceClose() throws TCDatabaseException {
    try {
      if (cpds == null) return;
      DataSources.destroy(cpds);
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  public synchronized boolean isOpen() {
    return status == DBEnvironmentStatus.STATUS_OPEN;
  }

  public File getEnvironmentHome() {
    return envHome;
  }

  public static final String getClusterStateStoreName() {
    return CLUSTER_STATE_STORE;
  }

  public synchronized TCObjectDatabase getObjectDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCObjectDatabase) tables.get(OBJECT_DB_NAME);
  }

  public synchronized TCBytesBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized TCBytesBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(OID_STORE_LOG_DB_NAME);
  }

  public synchronized TCRootDatabase getRootDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCRootDatabase) tables.get(ROOT_DB_NAME);
  }

  public TCLongDatabase getClientStateDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCLongDatabase) tables.get(CLIENT_STATE_DB_NAME);
  }

  public TCBytesBytesDatabase getTransactionDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCBytesToBlobDB) tables.get(TRANSACTION_DB_NAME);
  }

  public TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCIntToBytesDatabase) tables.get(CLASS_DB_NAME);
  }

  public TCMapsDatabase getMapsDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCMapsDatabase) tables.get(MAP_DB_NAME);
  }

  public TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCLongToStringDatabase) tables.get(STRING_INDEX_DB_NAME);
  }

  public TCStringToStringDatabase getClusterStateStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (DerbyTCStringToStringDatabase) tables.get(CLUSTER_STATE_STORE);
  }

  public MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger log, String sequenceID,
                                     int startValue) {
    try {
      return new DerbyDBSequence((DerbyPersistenceTransactionProvider) ptxp, sequenceID, startValue);
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public PersistenceTransactionProvider getPersistenceTransactionProvider() {
    return new DerbyPersistenceTransactionProvider(this);
  }

  public boolean isParanoidMode() {
    return isParanoid;
  }

  public void assertOpen() throws DatabaseNotOpenException {
    if (DBEnvironmentStatus.STATUS_OPEN != status) throw new DatabaseNotOpenException(
                                                                                      "Database environment should be open but isn't.");
  }
}
