/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

public class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  private final DerbyDBEnvironment             derbyDBEnv;
  private final static DerbyTransactionWrapper NULL_TX = new DerbyTransactionWrapper(null);

  public DerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    this.derbyDBEnv = derbyDBEnv;
  }

  public PersistenceTransaction newTransaction() {
    try {
      Connection connection = derbyDBEnv.createConnection();
      connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      return new DerbyTransactionWrapper(connection);
    } catch (TCDatabaseException e) {
      e.printStackTrace();
      throw new DBException(e);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DBException(e);
    }
  }

  public PersistenceTransaction nullTransaction() {
    return NULL_TX;
  }

}
