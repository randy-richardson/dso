/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCIntToBytesDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DerbyTCIntToBytesDatabase extends AbstractDerbyTCDatabase implements TCIntToBytesDatabase {

  public DerbyTCIntToBytesDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  @Override
  protected void createTableIfNotExists(Connection connection) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " " + DerbyDataTypes.TC_INT + ", " + VALUE
    + " " + DerbyDataTypes.TC_BYTE_ARRAY_VALUE + ", PRIMARY KEY(" + KEY + ") )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public byte[] get(int id, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setInt(1, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    } finally {
      closeResultSet(rs);
    }
  }

  public Map<Integer, byte[]> getAll(PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    Map<Integer, byte[]> map = new HashMap<Integer, byte[]>();
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        map.put(rs.getInt(1), rs.getBytes(2));
      }
      return map;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      try {
        closeResultSet(rs);
        connection.commit();
      } catch (SQLException e) {
        // Ignore
      }
    }
  }

  public boolean put(int id, byte[] b, PersistenceTransaction tx) {
    if (get(id, tx) == null) {
      return insert(id, b, tx);
    } else {
      return update(id, b, tx);
    }
  }

  private boolean update(int id, byte[] b, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, b);
      psUpdate.setInt(2, id);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private boolean insert(int id, byte[] b, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setInt(1, id);
      psPut.setBytes(2, b);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return true;
  }

}
