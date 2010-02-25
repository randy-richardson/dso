/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.sleepycat.je.Database;
import com.tc.objectserver.persistence.sleepycat.DatabaseOpenResult;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.io.File;

// This should be the class which should be used by Derby Db environment and Berkeley db env
public interface DBEnvironment {
  public abstract DatabaseOpenResult open() throws TCDatabaseException;

  public abstract void close() throws TCDatabaseException;

  public abstract boolean isOpen();

  public abstract File getEnvironmentHome();

  // TODO: See what has to be done for getStats and getEnvironment

  // TODO: See also getClassCatalogWrapper

  public abstract TCObjectDatabase getObjectDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException;

  public abstract TCRootDatabase getRootDatabase() throws TCDatabaseException;

  public abstract TCLongDatabase getClientStateDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getTransactionDatabase() throws TCDatabaseException;

  public abstract Database getGlobalSequenceDatabase() throws TCDatabaseException;

  public abstract TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException;

  public abstract Database getMapsDatabase() throws TCDatabaseException;

  public abstract Database getStringIndexDatabase() throws TCDatabaseException;

  public abstract Database getClusterStateStoreDatabase() throws TCDatabaseException;
}
