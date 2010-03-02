/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCLongToStringDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import gnu.trove.TLongObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyTCLongToStringDatabase extends AbstractDerbyTCDatabase implements TCLongToStringDatabase {

  public DerbyTCLongToStringDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  @Override
  protected void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " INT, " + VALUE + " VARCHAR (" + Integer.MAX_VALUE
                   + ") )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public TLongObjectHashMap loadMappingsInto(TLongObjectHashMap target, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        target.put(rs.getLong(1), rs.getString(2));
      }
      return target;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      try {
        connection.commit();
      } catch (SQLException e) {
        // Ignore
      }
    }
  }

  public boolean put(long id, String string, PersistenceTransaction tx) {
    if(get(id) == null) {
      return insert(id, string);
    }
    return false;
  }

  private boolean insert(long id, String b) {
    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setLong(1, id);
      psPut.setString(2, b);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return true;
  }
  
  private byte[] get(long id) {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setLong(1, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }
}
