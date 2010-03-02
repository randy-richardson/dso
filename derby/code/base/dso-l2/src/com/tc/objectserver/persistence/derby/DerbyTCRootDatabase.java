/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.TCRootDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DerbyTCRootDatabase extends AbstractDerbyTCDatabase implements TCRootDatabase {
  public DerbyTCRootDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  protected final void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " VARCHAR (32672) FOR BIT DATA, " + VALUE + " BIGINT )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public long get(byte[] rootName, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setBytes(1, rootName);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return ObjectID.NULL_ID.toLong(); }
      long temp = rs.getLong(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root", e);
    }
  }

  public Set<ObjectID> getRootIds(PersistenceTransaction tx) {
    ResultSet rs = null;
    Set<ObjectID> set = new HashSet<ObjectID>();
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(new ObjectID(rs.getLong(1)));
      }
      return set;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      try {
        connection.commit();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  public List<byte[]> getRootNames(PersistenceTransaction tx) {
    ResultSet rs = null;
    ArrayList<byte[]> list = new ArrayList<byte[]>();
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        list.add(rs.getBytes(1));
      }
      return list;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root ids", e);
    } finally {
      try {
        connection.commit();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx) {
    ResultSet rs = null;
    Map<byte[], Long> map = new HashMap<byte[], Long>();
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + ", " + VALUE + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        map.put(rs.getBytes(1), rs.getLong(2));
      }
      return map;
    } catch (SQLException e) {
      throw new DBException("Could not retrieve root map", e);
    } finally {
      try {
        connection.commit();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  public boolean put(byte[] rootName, long id, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setBytes(1, rootName);
      psPut.setLong(2, id);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException("Could not put root", e);
    }
    return true;
  }

}
