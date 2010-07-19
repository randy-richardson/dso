/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;
import com.sleepycat.util.RuntimeExceptionWrapper;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.berkeleydb.BerkeleyDBTCBytesBytesDatabase;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * XXX: This test fails
 */
public class SleepycatSerializationAdapterUnitTest extends TCTestCase {
  private BerkeleyDBEnvironment         env;
  private SleepycatSerializationAdapter ssa;
  private static int                    dbHomeCounter = 0;
  private static File                   tempDirectory;

  public void setUp() throws Exception {
    if (tempDirectory == null) {
      tempDirectory = getTempDirectory();
    }

    File dbHome = newDBHome();
    if (env != null) env.close();
    env = new BerkeleyDBEnvironment(true, dbHome);
    env.open();

    ClassCatalog catalog = env.getClassCatalogWrapper().getClassCatalog();
    ssa = new SleepycatSerializationAdapter(catalog);
  }

  public void test() throws Exception {
    PersistenceTransactionProvider ptp = env.getPersistenceTransactionProvider();
    Database db = ((BerkeleyDBTCBytesBytesDatabase) env.getObjectOidStoreDatabase()).getDatabase();
    byte[] VALUE = new byte[8];

    // Create a string key
    String rootName = "my-name-is-root";
    byte[] key = ssa.serializeString(rootName);
    PersistenceTransaction tx = ptp.newTransaction();
    Transaction bdbTx = ((TransactionWrapper) tx).getTransaction();

    if (!db.put(bdbTx, new DatabaseEntry(key), new DatabaseEntry(VALUE)).equals(OperationStatus.SUCCESS)) { throw new AssertionError(); }
    tx.commit();

    tx = ptp.newTransaction();
    bdbTx = ((TransactionWrapper) tx).getTransaction();
    List<byte[]> rv = new ArrayList<byte[]>();
    Cursor cursor = null;
    DatabaseEntry dbkey = new DatabaseEntry();
    DatabaseEntry dbValue = new DatabaseEntry();
    cursor = db.openCursor(bdbTx, CursorConfig.READ_COMMITTED);
    while (OperationStatus.SUCCESS.equals(cursor.getNext(dbkey, dbValue, LockMode.DEFAULT))) {
      rv.add(dbkey.getData());
    }
    cursor.close();
    tx.commit();

    Assert.assertEquals(1, rv.size());
    try {
      ssa.deserializeString(rv.get(0));
    } catch (RuntimeExceptionWrapper e) {
      e.printStackTrace();
      return;
    }
    throw new AssertionError();
  }

  private File newDBHome() {
    File file;
    // XXX: UGH... this extra increment is here because of a weird interaction with the static cache of the sleepycat
    // database and JUnit's test running junk, and something else I don't quite understand about the File.exists(). I
    // had to add this to ensure that the db counter was actually incremented and that a new directory was actually
    // used.
    ++dbHomeCounter;
    for (file = new File(tempDirectory, "db" + dbHomeCounter); file.exists(); ++dbHomeCounter) {
      //
    }
    assertFalse(file.exists());
    return file;
  }

}
