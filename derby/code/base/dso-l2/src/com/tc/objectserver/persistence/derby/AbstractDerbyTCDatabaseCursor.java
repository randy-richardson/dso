/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.sleepycat.DBException;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class AbstractDerbyTCDatabaseCursor<K, V> implements TCDatabaseCursor<K, V> {
  protected ResultSet rs;

  public AbstractDerbyTCDatabaseCursor(ResultSet rs) {
    this.rs = rs;
  }

  public void close() {
    try {
      rs.close();
    } catch (SQLException e) {
      throw new DBException(e);
    }
  }

  public void delete() {
    try {
      rs.deleteRow();
    } catch (SQLException e) {
      throw new DBException(e);
    }

  }

  public abstract boolean getNext(TCDatabaseEntry<K, V> entry);
}
