/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence;

import com.tc.object.ObjectID;
import com.tc.objectserver.persistence.TCDatabaseConstants.Status;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TCRootDatabase {
  public Status put(byte[] rootName, long id, PersistenceTransaction tx);

  public long get(byte[] rootName, PersistenceTransaction tx);

  public Map<byte[], Long> getRootNamesToId(PersistenceTransaction tx);

  public List<byte[]> getRootNames(PersistenceTransaction tx);
  
  public Set<ObjectID> getRootIds(PersistenceTransaction tx);
}
