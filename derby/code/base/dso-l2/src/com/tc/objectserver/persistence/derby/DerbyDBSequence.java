/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.util.UUID;
import com.tc.util.sequence.MutableSequence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DerbyDBSequence implements MutableSequence {
  public static final String  SEQUENCE_TABLE = "sequenceTable";
  private static final String SEUQENCE_NAME  = "sequenceName";
  private static final String SEQUENCE_UID   = "sequenceUid";
  private static final String SEQUENCE_VALUE = "sequenceValue";

  private final Connection    connection;
  private final String        entryName;
  private final String        uid;

  /**
   * Assuming connection is already open and sequence table has already been created.
   */
  public DerbyDBSequence(Connection connection, String entryName, int startValue) throws SQLException {
    this.connection = connection;
    this.entryName = entryName;
    if (startValue < 0) throw new IllegalArgumentException("start value cannot be < 0");

    createSequenceIfNeccesary(startValue);
    this.uid = setUID();
  }

  private String setUID() throws SQLException {
    ResultSet rs = null;
    PreparedStatement psSelect = connection.prepareStatement("SELECT " + SEQUENCE_UID + " FROM " + SEQUENCE_TABLE
                                                             + " WHERE " + SEUQENCE_NAME + " = ?");
    psSelect.setString(1, entryName);
    rs = psSelect.executeQuery();

    if (rs.next()) {
      String seqID = rs.getString(SEQUENCE_UID);
      rs.close();
      this.connection.commit();
      return seqID;
    }

    throw new IllegalStateException("Should never reach here");
  }

  public String getUID() {
    return this.uid;
  }

  public synchronized long nextBatch(long batchSize) {
    long current = current();
    setNext(current + batchSize);

    return current;
  }

  public synchronized void setNext(long next) {
    long current = current();
    if (current > next) { throw new AssertionError("Current = " + current + " Next = " + next); }

    try {
      PreparedStatement psUpdate = connection.prepareStatement("UPDATE " + SEQUENCE_TABLE + " SET " + SEQUENCE_VALUE
                                                               + " = ? " + " WHERE " + SEUQENCE_NAME + " = ?");
      psUpdate.setLong(1, next);
      psUpdate.setString(2, entryName);
      psUpdate.executeUpdate();

      connection.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized long current() {
    ResultSet rs = null;
    PreparedStatement psSelect;
    try {
      psSelect = connection.prepareStatement("SELECT " + SEQUENCE_VALUE + " FROM " + SEQUENCE_TABLE + " WHERE "
                                             + SEUQENCE_NAME + " = ?");
      psSelect.setString(1, entryName);
      rs = psSelect.executeQuery();

      if (!rs.next()) { throw new IllegalStateException("The value should always exist"); }

      long current = rs.getLong(1);
      rs.close();
      this.connection.commit();
      return current;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized long next() {
    return nextBatch(1);
  }

  public void createSequenceIfNeccesary(int startVal) throws SQLException {
    if (exists()) { return; }

    PreparedStatement psPut = connection.prepareStatement("INSERT INTO " + SEQUENCE_TABLE + " VALUES (?, ?, ?)");
    psPut.setString(1, entryName);
    psPut.setLong(2, startVal);
    psPut.setString(3, UUID.getUUID().toString());

    psPut.executeUpdate();
    psPut.close();
    connection.commit();
  }

  private boolean exists() throws SQLException {
    ResultSet rs = null;
    PreparedStatement psSelect = connection.prepareStatement("SELECT " + SEQUENCE_VALUE + " FROM " + SEQUENCE_TABLE
                                                             + " WHERE " + SEUQENCE_NAME + " = ?");
    psSelect.setString(1, entryName);
    rs = psSelect.executeQuery();

    if (!rs.next()) {
      rs.close();
      this.connection.commit();
      return false;
    }
    return true;
  }

  /**
   * This method should be called atleast once so that the sequence table exists before creating 1 sequence.
   */
  public static void createSequenceTable(Connection connection) throws SQLException {
    if (tableExists(connection)) { return; }

    Statement statement = connection.createStatement();
    String query = "CREATE TABLE " + SEQUENCE_TABLE + "(" + SEUQENCE_NAME + " CHAR (50), " + SEQUENCE_VALUE
                   + " BIGINT, " + SEQUENCE_UID + " CHAR (100))";
    statement.execute(query);
    statement.close();
    connection.commit();
  }

  private static boolean tableExists(Connection connection) throws SQLException {
    DatabaseMetaData dbmd = connection.getMetaData();

    String[] types = { "TABLE" };
    ResultSet resultSet = dbmd.getTables(null, null, "%", types);
    while (resultSet.next()) {
      String tableName = resultSet.getString(3);
      if (tableName.equalsIgnoreCase(SEQUENCE_TABLE)) {
        resultSet.close();
        connection.commit();
        return true;
      }
    }
    return false;
  }
}
