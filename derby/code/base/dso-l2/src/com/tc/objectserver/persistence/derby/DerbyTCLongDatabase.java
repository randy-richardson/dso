/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCLongDatabase;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class DerbyTCLongDatabase extends AbstractDerbyTCDatabase implements TCLongDatabase {

  public DerbyTCLongDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  @Override
  protected void createTableIfNotExists(Connection connection) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " " + DerbyDataTypes.TC_LONG + ", PRIMARY KEY(" + KEY
                   + ") )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public boolean contains(long key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName + " WHERE " + KEY
                                                               + " = ?");
      psSelect.setLong(1, key);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return false; }
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    } finally {
      closeResultSet(rs);
    }
  }

  public Status delete(long key, PersistenceTransaction tx) {
    if (!contains(key, tx)) { return Status.NOT_FOUND; }
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setLong(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Set<Long> getAllKeys(PersistenceTransaction tx) {
    ResultSet rs = null;
    Set<Long> set = new HashSet<Long>();
    Connection connection = pt2nt(tx);
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(rs.getLong(1));
      }
      return set;
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

  public boolean put(long key, PersistenceTransaction tx) {
    if (contains(key, tx)) {
      return false;
    } else {
      return insert(key, tx);
    }
  }

  private boolean insert(long key, PersistenceTransaction tx) {
    PreparedStatement psPut;
    Connection connection = pt2nt(tx);
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?)");
      psPut.setLong(1, key);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return true;
  }

}
