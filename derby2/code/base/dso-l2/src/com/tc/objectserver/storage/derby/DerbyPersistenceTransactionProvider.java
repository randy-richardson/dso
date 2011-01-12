/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.derby;

import com.tc.objectserver.persistence.db.DBException;
import com.tc.objectserver.persistence.db.TCDatabaseException;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.objectserver.storage.api.PersistenceTransactionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  protected final DerbyDBEnvironment             derbyDBEnv;
  protected final static DerbyTransactionWrapper NULL_TX = new DerbyTransactionWrapper(null);

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
