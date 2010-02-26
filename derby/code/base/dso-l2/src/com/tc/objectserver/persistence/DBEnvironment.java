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
  public abstract DatabaseOpenResult open() throws TCDatabaseException;

  public abstract void close() throws TCDatabaseException;

  public abstract boolean isOpen();

  public abstract File getEnvironmentHome();
  
  public abstract boolean isParanoidMode();
  
  public abstract PersistenceTransactionProvider getPersistenceTransactionProvider();

  // TODO: See what has to be done for getStats and getEnvironment

  // TODO: See also getClassCatalogWrapper

  public abstract TCObjectDatabase getObjectDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getObjectOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getMapsOidStoreDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getOidStoreLogDatabase() throws TCDatabaseException;

  public abstract TCRootDatabase getRootDatabase() throws TCDatabaseException;

  public abstract TCLongDatabase getClientStateDatabase() throws TCDatabaseException;

  public abstract TCBytesBytesDatabase getTransactionDatabase() throws TCDatabaseException;

  public abstract TCIntToBytesDatabase getClassDatabase() throws TCDatabaseException;

  public abstract TCMapsDatabase getMapsDatabase() throws TCDatabaseException;

  public abstract TCLongToStringDatabase getStringIndexDatabase() throws TCDatabaseException;

  public abstract TCStringToStringDatabase getClusterStateStoreDatabase() throws TCDatabaseException;

  public abstract MutableSequence getSequence(PersistenceTransactionProvider ptxp, TCLogger logger, String sequenceID,
                                              int startValue);
}
