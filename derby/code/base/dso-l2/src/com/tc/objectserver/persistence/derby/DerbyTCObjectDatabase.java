/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.TCObjectDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;
import com.tc.util.ObjectIDSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyTCObjectDatabase extends AbstractDerbyTCDatabase implements TCObjectDatabase {
  private static final TCLogger logger = TCLogging.getLogger(DerbyTCObjectDatabase.class);

  public DerbyTCObjectDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  protected final void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " BIGINT, " + VALUE + " BLOB (16M) )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public boolean delete(long id, PersistenceTransaction tx) {
    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setLong(1, id);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public byte[] get(long id, PersistenceTransaction tx) {
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
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    }
  }

  public ObjectIDSet getAllObjectIds(PersistenceTransaction tx) {
    ResultSet rs = null;
    ObjectIDSet set = new ObjectIDSet();
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + " FROM " + tableName);
      rs = psSelect.executeQuery();

      while (rs.next()) {
        set.add(new ObjectID(rs.getLong(1)));
      }
      return set;
    } catch (Throwable e) {
      logger.error("Error Reading Object IDs", e);
    } finally {
      try {
        connection.commit();
      } catch (SQLException e) {
        // Ignore
      }
    }
    return null;
  }

  public boolean put(long id, byte[] b, PersistenceTransaction tx) {
    if (get(id, tx) == null) {
      return insert(id, b);
    } else {
      return update(id, b);
    }
  }

  private boolean update(long id, byte[] b) {
    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, b);
      psUpdate.setLong(2, id);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private boolean insert(long id, byte[] b) {
    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setLong(1, id);
      psPut.setBytes(2, b);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return true;
  }
}
