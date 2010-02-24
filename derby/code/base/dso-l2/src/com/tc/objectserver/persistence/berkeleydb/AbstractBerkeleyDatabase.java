/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.berkeleydb;

import com.sleepycat.je.Database;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.sleepycat.TransactionWrapper;

public abstract class AbstractBerkeleyDatabase {
  protected final Database db;

  public AbstractBerkeleyDatabase(Database db) {
    this.db = db;
  }

  protected Transaction pt2nt(PersistenceTransaction tx) {
    return (tx instanceof TransactionWrapper) ? ((TransactionWrapper) tx).getTransaction() : null;
  }

  public TCDatabaseCursor openCursor(PersistenceTransaction tx) {
    throw new UnsupportedOperationException();
  }

  public final Database getDatabase() {
    return db;
  }
}
