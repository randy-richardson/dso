/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.TCStringToStringDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyTCStringToStringDatabase extends AbstractDerbyTCDatabase implements TCStringToStringDatabase {

  public DerbyTCStringToStringDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  @Override
  protected void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " VARCHAR (" + Integer.MAX_VALUE + ") , " + VALUE
                   + " VARCHAR (" + Integer.MAX_VALUE + ") )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public Status delete(String key, PersistenceTransaction tx) {
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    entry.setKey(key);
    Status status = get(entry, tx);
    if(status != Status.SUCCESS) {
      return status;
    }
    
    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setString(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status get(TCDatabaseEntry<String, String> entry, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setString(1, entry.getKey());
      rs = psSelect.executeQuery();

      if (!rs.next()) { return Status.NOT_FOUND; }
      entry.setValue(rs.getString(1));
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public boolean put(String key, String value, PersistenceTransaction tx) {
    TCDatabaseEntry<String, String> entry = new TCDatabaseEntry<String, String>();
    entry.setKey(key);
    Status status = get(entry, tx);
    if(status == Status.SUCCESS) {
      return insert(key, value);
    } else {
      return update(key, value);
    }

  }

  private boolean update(String key, String value) {
    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setString(1, value);
      psUpdate.setString(2, key);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private boolean insert(String key, String value) {
    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setString(1, value);
      psPut.setString(2, key);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return true;
  }
}
