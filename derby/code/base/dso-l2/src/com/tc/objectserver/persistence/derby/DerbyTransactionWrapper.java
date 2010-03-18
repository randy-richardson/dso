/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DerbyTransactionWrapper implements PersistenceTransaction {
  private final Connection connection;
  private final Map        properties = new HashMap(1);

  public DerbyTransactionWrapper(Connection conn) {
    this.connection = conn;
  }

  public void abort() {
    try {
      connection.rollback();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public void commit() {
    try {
      connection.commit();
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeConnection();
    }
  }

  /**
   * This is done to return the connection to the connection pool
   */
  private void closeConnection() {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Object getProperty(Object key) {
    return properties.get(key);
  }

  public Object setProperty(Object key, Object value) {
    return properties.put(key, value);
  }

  public Connection getConnection() {
    return connection;
  }
}
