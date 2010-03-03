/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;

import java.sql.Connection;

public class DerbyPersistenceTransactionProvider implements PersistenceTransactionProvider {
  private final Connection                     connection;
  private final static DerbyTransactionWrapper NULL_TX = new DerbyTransactionWrapper(null);

  public DerbyPersistenceTransactionProvider(Connection connection) {
    this.connection = connection;
  }

  public PersistenceTransaction newTransaction() {
    return new DerbyTransactionWrapper(connection);
  }

  public PersistenceTransaction nullTransaction() {
    return NULL_TX;
  }

}
