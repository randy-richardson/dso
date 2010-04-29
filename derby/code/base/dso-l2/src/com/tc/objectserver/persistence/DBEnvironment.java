/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.logging.TCLogger;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.DatabaseOpenResult;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.util.sequence.MutableSequence;

import java.io.File;

// This should be the class which should be used by Derby Db environment and Berkeley db env
public interface DBEnvironment {
  static final String GLOBAL_SEQUENCE_DATABASE = "global_sequence_db";
  static final String ROOT_DB_NAME             = "roots";
  static final String OBJECT_DB_NAME           = "objects";
  static final String OBJECT_OID_STORE_DB_NAME = "objects_oid_store";
  static final String MAPS_OID_STORE_DB_NAME   = "mapsdatabase_oid_store";
  static final String OID_STORE_LOG_DB_NAME    = "oid_store_log";

  static final String CLIENT_STATE_DB_NAME     = "clientstates";
  static final String TRANSACTION_DB_NAME      = "transactions";
  static final String STRING_INDEX_DB_NAME     = "stringindex";
  static final String CLASS_DB_NAME            = "classdefinitions";
  static final String MAP_DB_NAME              = "mapsdatabase";
  static final String CLUSTER_STATE_STORE      = "clusterstatestore";
  static final String CONTROL_DB               = "controldb";

  enum DBEnvironmentStatus {
    STATUS_INIT, STATUS_ERROR, STATUS_OPENING, STATUS_OPEN, STATUS_CLOSING, STATUS_CLOSED
  }

  public abstract DatabaseOpenResult open() throws TCDatabaseException;

  public abstract void close() throws TCDatabaseException;

  public abstract boolean isOpen();

  public abstract File getEnvironmentHome();

  public abstract boolean isParanoidMode();

  public abstract PersistenceTransactionProvider getPersistenceTransactionProvider();

  public abstract TCObjectDatabase getObjectDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException;

  public abstract TCRootDatabase getRootDatabase() throws TCDatabaseException;

  public abstract TCLongDatabase getClientStateDatabase() throws TCDatabaseException;

  public abstract TCBytesToBytesDatabase getTransactionDatabase() throws TCDatabaseException;

  public abstract TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException;

  public abstract TCMapsDatabase getMapsDatabase() throws TCDatabaseException;

  public abstract TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException;

  public abstract TCStringToStringDatabase getClusterStateStoreDatabase() throws TCDatabaseException;

  public abstract MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger logger, String sequenceID,
                                              int startValue);
}
