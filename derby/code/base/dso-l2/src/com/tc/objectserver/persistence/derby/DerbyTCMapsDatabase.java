/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.derby.DerbyTCBytesToBlobDB.DerbyTCBytesBytesCursor;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.TCDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyTCMapsDatabase extends AbstractDerbyTCDatabase implements TCMapsDatabase {
  private static final String OBJECT_ID = "objectid";

  public DerbyTCMapsDatabase(String tableName, Connection connection) throws TCDatabaseException {
    super(tableName, connection);
  }

  @Override
  protected void createTableIfNotExists(Connection connection) throws SQLException {
    if (DerbyDBEnvironment.tableExists(connection, tableName)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + tableName + "(" + OBJECT_ID + " BIGINT, " + KEY
                   + " VARCHAR (32672) FOR BIT DATA, " + VALUE + " BLOB (16M), PRIMARY KEY(" + KEY + "," + OBJECT_ID
                   + ") )";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  public boolean delete(long objectID, byte[] key, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + KEY + " = ?");
      psUpdate.setBytes(1, key);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  /**
   * TODO: This should return no of bytes and not no of rows updated.
   */
  public int deleteCollection(long objectID, PersistenceTransaction tx) throws TCDatabaseException {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("DELETE FROM " + tableName + " WHERE " + OBJECT_ID
                                                               + " = ?");
      psUpdate.setLong(1, objectID);
      int recordsUpdated = psUpdate.executeUpdate();
      return recordsUpdated;
    } catch (SQLException e) {
      throw new TCDatabaseException(e);
    }
  }

  public TCDatabaseCursor<byte[], byte[]> openCursor(PersistenceTransaction tx, long objectID) {
    try {
      Connection connection = pt2nt(tx);
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + KEY + "," + VALUE + " FROM " + tableName
                                                               + " WHERE " + OBJECT_ID + " = ?");
      psSelect.setLong(1, objectID);
      return new DerbyTCBytesBytesCursor(psSelect.executeQuery());
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public boolean put(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    if (!contains(id, k, tx)) {
      return insert(id, k, v, tx);
    } else {
      return update(id, k, v, tx);
    }
  }

  private boolean contains(long id, byte[] k, PersistenceTransaction tx) {
    ResultSet rs = null;
    Connection connection = pt2nt(tx);
    try {
      PreparedStatement psSelect = connection.prepareStatement("SELECT " + VALUE + " FROM " + tableName + " WHERE "
                                                               + KEY + " = ? AND " + OBJECT_ID + " = ? ");
      psSelect.setBytes(1, k);
      psSelect.setLong(2, id);
      rs = psSelect.executeQuery();

      if (!rs.next()) { return false; }
      return true;
    } catch (SQLException e) {
      throw new DBException("Error retrieving object id: " + id + "; error: " + e.getMessage());
    } finally {
      closeResultSet(rs);
    }
  }

  private boolean update(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + tableName + " SET " + VALUE + " = ? "
                                                               + " WHERE " + KEY + " = ? AND " + OBJECT_ID + " = ? ");
      psUpdate.setBytes(1, v);
      psUpdate.setBytes(2, k);
      psUpdate.setLong(3, id);
      psUpdate.executeUpdate();
      return true;
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  private boolean insert(long id, byte[] k, byte[] v, PersistenceTransaction tx) {
    Connection connection = pt2nt(tx);

    PreparedStatement psPut;
    try {
      psPut = connection.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?, ?)");
      psPut.setLong(1, id);
      psPut.setBytes(2, k);
      psPut.setBytes(3, v);
      psPut.executeUpdate();
    } catch (SQLException e) {
      throw new DBException(e);
    }
    return true;
  }
}
