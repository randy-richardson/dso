/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

import java.sql.Connection;

public class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  protected final DerbyDBEnvironment                derbyDBEnv;
  protected final static DerbyTransactionWrapper    NULL_TX        = new DerbyTransactionWrapper(null);

  private final ThreadLocal<PersistenceTransaction> threadLocalTxn = new ThreadLocal<PersistenceTransaction>() {
                                                                     @Override
                                                                     protected PersistenceTransaction initialValue() {
                                                                       return createNewTransaction();
                                                                     }
                                                                   };

  public DerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    this.derbyDBEnv = derbyDBEnv;
  }

  public PersistenceTransaction newTransaction() {
    return threadLocalTxn.get();
  }

  public PersistenceTransaction createNewTransaction() {
    try {
      Connection connection = derbyDBEnv.createConnection();
      return new DerbyTransactionWrapper(connection);
    } catch (Exception e) {
      throw new DBException(e);
    }
  }

  public PersistenceTransaction nullTransaction() {
    return NULL_TX;
  }
}
