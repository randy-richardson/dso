/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCBytesToBytesDatabase;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyTCBytesToBlobDB extends AbstractDerbyTCDatabase implements TCBytesToBytesDatabase {
  public DerbyTCBytesToBlobDB(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  protected final void createTableIfNotExists(Connection connection) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + KEY + " " + DerbyDataTypes.TC_BYTE_ARRAY_KEY + ", " + VALUE
                   + " " + DerbyDataTypes.TC_BYTE_ARRAY_VALUE + ", PRIMARY KEY(" + KEY + ") )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public Status delete(byte[] key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public byte[] get(byte[] key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

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
    } finally {
      closeResultSet(rs);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursorUpdatable(PersistenceTransaction tx) {
    try {
      Connection connection = pt2nt(tx);
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName,
                                                               ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                               ResultSet.CONCUR_UPDATABLE);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx) {
    try {
      Connection connection = pt2nt(tx);
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status put(byte[] key, byte[] val, PersistenceTransaction tx) {
    if (get(key, tx) == null) {
      return insert(key, val, tx);
    } else {
      return update(key, val, tx);
    }
  }

  private Status update(byte[] key, byte[] val, PersistenceTransaction tx) {
    try {
      Connection connection = pt2nt(tx);
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, val);
      psUpdate.setBytes(2, key);
      psUpdate.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private Status insert(byte[] key, byte[] val, PersistenceTransaction tx) {
    PreparedStatement psPut;
    try {
      Connection connection = pt2nt(tx);
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
      psPut.setBytes(1, key);
      psPut.setBytes(2, val);
      psPut.executeUpdate();
      return Status.SUCCESS;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public Status putNoOverwrite(PersistenceTransaction tx, byte[] key, byte[] value) {
    if (get(key, tx) == null) { return insert(key, value, tx); }
    return Status.NOT_SUCCESS;
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
