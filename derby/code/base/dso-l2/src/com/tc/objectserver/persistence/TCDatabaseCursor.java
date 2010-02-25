/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

public interface TCDatabaseCursor<K, V> {
  public boolean getNext(TCDatabaseEntry<K, V> entry);
  
  public boolean getSearchKeyRange(TCDatabaseEntry<K, V> entry);

  public void delete();

  public void close();
}
