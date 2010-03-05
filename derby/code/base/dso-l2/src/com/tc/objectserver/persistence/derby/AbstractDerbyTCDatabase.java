/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractDerbyTCDatabase {
  protected final static String KEY    = "derbykey";
  protected final static String VALUE  = "derbyvalue";

  protected final String        tableName;
  private static final TCLogger logger = TCLogging.getLogger(AbstractDerbyTCDatabase.class);

  public AbstractDerbyTCDatabase(String tableName, Connection connection) throws TCDatabaseException {
    this.tableName = tableName;
    try {
      createTableIfNotExists(connection);
    } catch (SQLException e) {
      try {
        connection.rollback();
      } catch (SQLException e1) {
        throw new TCDatabaseException(e1);
      }
      e.printStackTrace();
      throw new TCDatabaseException(e);
    }
  }

  protected Connection pt2nt(PersistenceTransaction tx) {
    return (tx instanceof DerbyTransactionWrapper) ? ((DerbyTransactionWrapper) tx).getConnection() : null;
  }

  protected void closeResultSet(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        logger.info(e.getMessage(), e);
      }
    }
  }

  protected abstract void createTableIfNotExists(Connection connection) throws SQLException;
}
