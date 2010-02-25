/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.io.IOException;

public interface PersistableCollection {

  public int commit(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db)
      throws IOException, TCDatabaseException;

  public void load(SleepycatCollectionsPersistor persistor, PersistenceTransaction tx, TCMapsDatabase db) throws IOException,
      ClassNotFoundException, TCDatabaseException;
}
