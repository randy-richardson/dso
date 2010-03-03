/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCBytesBytesDatabase;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyTCBytesToBlobDB extends AbstractDerbyTCDatabase implements TCBytesBytesDatabase {
  public DerbyTCBytesToBlobDB(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  protected final void createTableIfNotExists() throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " VARCHAR (32672) FOR BIT DATA, " + VALUE
                   + " BLOB (16M) )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public boolean delete(byte[] key, PersistenceTransaction tx) {
    return false;
  }

  public byte[] get(byte[] key, PersistenceTransaction tx) {
    ResultSet rs = null;
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ?");
      psSelect.setBytes(1, key);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return null; }
      byte[] temp = rs.getBytes(1);
      return temp;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx) {
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public boolean put(byte[] key, byte[] val, PersistenceTransaction tx) {
    if (get(key, tx) == null) {
      return insert(key, val);
    } else {
      return update(key, val);
    }
  }

  private boolean update(byte[] key, byte[] val) {
    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, val);
      psUpdate.setBytes(2, key);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private boolean insert(byte[] key, byte[] val) {
    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setBytes(1, key);
      psPut.setBytes(2, val);
      psPut.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public boolean putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value) {
    if (get(key, tx) == null) { return insert(key, value); }
    return false;
  }

  static class DerbyTCBytesBytesCursor extends AbstractDerbyTCDatabaseCursor<byte[], byte[]> {
    public DerbyTCBytesBytesCursor(ResultSet rs) {
      super(rs);
    }

    @Override
    public boolean getNext(TCDatabaseEntry<byte[], byte[]> entry) {
      try {
        if (rs.next()) {
          entry.setKey(rs.getBytes(1)).setValue(rs.getBytes(2));
          return true;
        }
      } catch (SQLException e) {
        throw new DBException(e);
      }
      return false;
    }

  }
}
