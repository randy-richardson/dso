/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

public class NullDerbyPersistenceTransactionProvider extends DerbyPersistenceTransactionProvider {

  public NullDerbyPersistenceTransactionProvider(DerbyDBEnvironment derbyDBEnv) {
    super(derbyDBEnv);
  }

  public PersistenceTransaction newTransaction() {
    try {
      Connection connection = derbyDBEnv.createConnection();
      connection.setTransactionIsolation(Connection.TRANSACTION_NONE);
      return new DerbyTransactionWrapper(connection);
    } catch (TCDatabaseException e) {
      e.printStackTrace();
      throw new DBException(e);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DBException(e);
    }
  }
}
