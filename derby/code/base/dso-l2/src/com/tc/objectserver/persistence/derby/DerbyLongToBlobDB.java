/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * this class is for object db
 */
public class DerbyLongToBlobDB {
  private final static String KEY   = "key";
  private final static String VALUE = "value";

  private final String        tableName;
  private final Connection    connection;

  public DerbyLongToBlobDB(String tableName, Connection connection) throws SQLException {
    this.tableName = tableName;
    this.connection = connection;
    createTableIfNotExists();
  }

  private void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " BIGINT, " + VALUE + " BLOB (16M) )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public void put(long key, byte[] val) throws SQLException {
    PreparedStatement psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
    psPut.setLong(1, key);
    psPut.setBytes(2, val);
    psPut.executeUpdate();
    connection.commit();
  }

  public byte[] get(long key) throws SQLException {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setLong(1, key);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } finally {
      rs.close();
      connection.commit();
    }
  }
}
