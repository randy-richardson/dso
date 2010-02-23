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
 * This class is for control db
 */
public class DerbyControlDB {
  private final static String KEY        = "key";
  private final static String VALUE      = "value";

  private final static short  CLEAN_FLAG = 1;
  private final static short  DIRTY_FLAG = 1;

  private final String        tableName;
  private final Connection    connection;

  public DerbyControlDB(String tableName, Connection connection) throws SQLException {
    this.tableName = tableName;
    this.connection = connection;
    createTableIfNotExists();
  }

  private void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " CHAR (10), " + VALUE + " SMALLINT )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public boolean isClean() throws SQLException {
    int flag = getFlag();
    return flag == CLEAN_FLAG || flag == -1;
  }

  public void setClean() throws SQLException {
    short flag = getFlag();
    if (flag == CLEAN_FLAG) {
      return;
    } else if (flag == -1) {
      insert(CLEAN_FLAG);
    } else {
      update(CLEAN_FLAG);
    }
  }

  public void setDirty() throws SQLException {
    short flag = getFlag();
    if (flag == DIRTY_FLAG) {
      return;
    } else if (flag == -1) {
      insert(DIRTY_FLAG);
    } else {
      update(DIRTY_FLAG);
    }
  }

  private void insert(short flag) throws SQLException {
    PreparedStatement psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
    psPut.setString(1, KEY);
    psPut.setShort(2, flag);

    psPut.executeUpdate();
    connection.commit();
  }

  private void update(short flag) throws SQLException {
    PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                             + " WHERE " + KEY + " = ?");
    psUpdate.setShort(1, flag);
    psUpdate.setString(2, KEY);
    psUpdate.executeUpdate();

    connection.commit();
  }

  private short getFlag() throws SQLException {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setString(1, KEY);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return -1; }
      short shortVal = rs.getShort(1);
      return shortVal;
    } finally {
      rs.close();
      connection.commit();
    }
  }
}
