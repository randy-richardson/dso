/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class AbstractDerbyTCDatabase {
  protected final static String KEY   = "key";
  protected final static String VALUE = "value";

  protected final String        tableName;
  protected final Connection    connection;

  public AbstractDerbyTCDatabase(String tableName, Connection connection) throws TCDatabaseException {
    this.tableName = tableName;
    this.connection = connection;
    try {
      createTableIfNotExists();
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  protected abstract void createTableIfNotExists() throws SQLException;
}
