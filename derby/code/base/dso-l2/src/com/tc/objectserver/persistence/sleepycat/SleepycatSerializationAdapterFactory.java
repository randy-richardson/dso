/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;

public class SleepycatSerializationAdapterFactory implements SerializationAdapterFactory {
  private final BerkeleyDBEnvironment dbEnv;

  public SleepycatSerializationAdapterFactory(BerkeleyDBEnvironment dbEnv) {
    this.dbEnv = dbEnv;
  }

  private ClassCatalog getClassCatalog() {
    try {
      return dbEnv.getClassCatalogWrapper().getClassCatalog();
    } catch (TCDatabaseException e) {
      throw new DBException(e);
    }
  }

  public SerializationAdapter newAdapter() {
    return new SleepycatSerializationAdapter(getClassCatalog());
  }

}
