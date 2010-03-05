/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.derby;

import com.tc.objectserver.persistence.sleepycat.SleepycatSequenceKeys;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;

public class DerbyDBSequenceTest extends TCTestCase {
  public static final String DRIVER     = "org.apache.derby.jdbc.EmbeddedDriver";
  public static final String PROTOCOL   = "jdbc:derby:";
  public static final String DB_NAME    = "objectDB";

  private File               envHome;
  private static int         count      = 0;

  private DerbyDBEnvironment env;

  protected void setUp() throws Exception {
    super.setUp();

    while ((envHome = new File(this.getTempDirectory(), ++count + "")).exists()) {
      //
    }
    System.out.println("DB home: " + envHome);
    open();
  }

  public void open() throws Exception {
    env = new DerbyDBEnvironment(true, envHome);
    env.open();
  }

  public void tearDown() throws Exception {
    super.tearDown();
    envHome.delete();
    env.close();
  }

  public void testUID() throws Exception {
    DerbyDBSequence sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env
        .getPersistenceTransactionProvider(), SleepycatSequenceKeys.CLIENTID_SEQUENCE_NAME, 0);
    String uid1 = sequence.getUID();
    assertNotNull(uid1);
    System.err.println("UID is " + uid1);
    sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env.getPersistenceTransactionProvider(),
                                   SleepycatSequenceKeys.CLIENTID_SEQUENCE_NAME, 0);

    String uid2 = sequence.getUID();
    System.err.println("UID is " + uid2);
    assertEquals(uid1, uid2);

    sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env.getPersistenceTransactionProvider(),
                                   SleepycatSequenceKeys.TRANSACTION_SEQUENCE_DB_NAME, 0);

    String uid3 = sequence.getUID();
    System.err.println("UID is " + uid3);
    assertNotEquals(uid1, uid3);
  }

  public void testBasic() throws Exception {
    DerbyDBSequence sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env
        .getPersistenceTransactionProvider(), SleepycatSequenceKeys.CLIENTID_SEQUENCE_NAME, 1);

    assertEquals(1, sequence.current());
    long id = sequence.next();

    assertEquals(1, id);
    id = sequence.nextBatch(100);
    assertEquals(2, id);
    id = sequence.next();
    assertEquals(102, id);
    id = sequence.next();
    assertEquals(103, id);
    id = sequence.next();
    assertEquals(104, id);
    sequence.setNext(1000);
    id = sequence.next();
    assertEquals(1000, id);
    id = sequence.nextBatch(100);
    assertEquals(1001, id);
    id = sequence.nextBatch(100);
    assertEquals(1101, id);
    boolean failed = false;
    try {
      sequence.setNext(100);
      failed = true;
    } catch (AssertionError er) {
      // expected
    }
    id = sequence.next();
    assertEquals(1201, id);
    id = sequence.current();
    assertEquals(1202, id);
    if (failed) { throw new AssertionError("Didn't fail"); }

    closeDBAndCheckSequence();
  }

  public void testLongBatchSize() throws Exception {
    DerbyDBSequence sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env
        .getPersistenceTransactionProvider(), SleepycatSequenceKeys.CLIENTID_SEQUENCE_NAME, 1);

    long id = sequence.next();
    assertEquals(1, id);
    long batchSize = Integer.MAX_VALUE * 2L;
    System.out.println("Testing with batch size = " + batchSize);
    id = sequence.nextBatch(batchSize);
    assertEquals(2, id);
    id = sequence.next();
    assertEquals(batchSize + 2, id);

    closeDBAndCheckSequence();
  }

  private void closeDBAndCheckSequence() throws Exception {
    String KEY_NAME = "SEQUENCE_TEST";

    DerbyDBSequence sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env
        .getPersistenceTransactionProvider(), KEY_NAME, 1);

    for (int i = 0; i < 10; i++) {
      sequence.nextBatch(2);
    }

    this.env.close();
    this.open();

    sequence = new DerbyDBSequence((DerbyPersistenceTransactionProvider) env.getPersistenceTransactionProvider(),
                                   KEY_NAME, 1);
    long seqnum = sequence.nextBatch(2);
    Assert.assertEquals(21, seqnum);
  }

}
