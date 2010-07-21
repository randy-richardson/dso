/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.io.serializer.DSOSerializerPolicy;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.api.BasicSerializer;
import com.tc.logging.TCLogger;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.managedobject.PersistableObjectState;
import com.tc.objectserver.persistence.TCDatabaseCursor;
import com.tc.objectserver.persistence.TCDatabaseEntry;
import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.SortedSet;

public class SleepycatCollectionsPersistor extends SleepycatPersistorBase {

  private final TCMapsDatabase             database;
  private static final int                 DELETE_BATCH_SIZE = TCPropertiesImpl
                                                                 .getProperties()
                                                                 .getInt(
                                                                         TCPropertiesConsts.L2_OBJECTMANAGER_DELETEBATCHSIZE,
                                                                         5000);
  private final BasicSerializer            serializer;
  private final ByteArrayOutputStream      bao;
  private final SleepycatCollectionFactory collectionFactory;
  private final TCObjectOutputStream       oo;

  public SleepycatCollectionsPersistor(final TCLogger logger, final TCMapsDatabase mapsDatabase,
                                       final SleepycatCollectionFactory sleepycatCollectionFactory) {
    this.database = mapsDatabase;
    this.collectionFactory = sleepycatCollectionFactory;
    final DSOSerializerPolicy policy = new DSOSerializerPolicy();
    this.serializer = new BasicSerializer(policy);
    this.bao = new ByteArrayOutputStream(1024);
    this.oo = new TCObjectOutputStream(this.bao);
  }

  public int saveCollections(final PersistenceTransaction tx, final ManagedObjectState state) throws IOException,
      TCDatabaseException {
    final PersistableObjectState persistabeState = (PersistableObjectState) state;
    final PersistableCollection collection = persistabeState.getPersistentCollection();
    return collection.commit(this, tx, this.database);
  }

  public synchronized byte[] serialize(final long id, final Object o) throws IOException {
    this.oo.writeLong(id);
    this.serializer.serializeTo(o, this.oo);
    this.oo.flush();
    final byte b[] = this.bao.toByteArray();
    this.bao.reset();
    return b;
  }

  public synchronized byte[] serialize(final Object o) throws IOException {
    this.serializer.serializeTo(o, this.oo);
    this.oo.flush();
    final byte b[] = this.bao.toByteArray();
    this.bao.reset();
    return b;
  }

  public void loadCollectionsToManagedState(final PersistenceTransaction tx, final ObjectID id,
                                            final ManagedObjectState state) throws IOException, ClassNotFoundException,
      TCDatabaseException {
    Assert.assertTrue(PersistentCollectionsUtil.isPersistableCollectionType(state.getType()));

    final PersistableObjectState persistableState = (PersistableObjectState) state;
    Assert.assertNull(persistableState.getPersistentCollection());
    final PersistableCollection collection = PersistentCollectionsUtil
        .createPersistableCollection(id, this.collectionFactory, state.getType());
    collection.load(this, tx, this.database);
    persistableState.setPersistentCollection(collection);
  }

  public Object deserialize(final int start, final byte[] data) throws IOException, ClassNotFoundException {
    if (start >= data.length) { return null; }
    final ByteArrayInputStream bai = new ByteArrayInputStream(data, start, data.length - start);
    final ObjectInput ois = new TCObjectInputStream(bai);
    return this.serializer.deserializeFrom(ois);
  }

  public Object deserialize(final byte[] data) throws IOException, ClassNotFoundException {
    return deserialize(0, data);
  }

  /**
   * This method is slightly dubious in that it assumes that the ObjectID is the first 8 bytes of the Key in the entire
   * collections database.(which is true, but that logic is spread elsewhere)
   * 
   * @param ptp - PersistenceTransactionProvider
   * @param oids - Object IDs to delete
   * @param extantMapTypeOidSet - a copy of the map OIDs
   * @throws TCDatabaseException
   */
  public long deleteAllCollections(final PersistenceTransactionProvider ptp, final SortedSet<ObjectID> oids,
                                   final SortedSet<ObjectID> extantMapTypeOidSet) throws TCDatabaseException {

    PersistenceTransaction tx = ptp.newTransaction();
    long totalEntriesDeleted = 0;
    int mapEntriesDeleted = 0;
    int accumulatedDeletes = 0;

    try {
      for (final ObjectID id : oids) {
        if (!extantMapTypeOidSet.contains(id)) {
          // Not a map type
          continue;
        }

        while (true) {
          mapEntriesDeleted = markForDeletion(id, tx);
          totalEntriesDeleted += mapEntriesDeleted;
          accumulatedDeletes += mapEntriesDeleted;
          if (accumulatedDeletes >= DELETE_BATCH_SIZE) {
            tx.commit();
            accumulatedDeletes = 0;
            tx = ptp.newTransaction();
          } else {
            break;
          }
        }
      }
    } finally {
      // probably a good idea to commit irrespective of mapEntriesDeleted
      tx.commit();
    }
    return totalEntriesDeleted;
  }

  /**
   * <p>
   * These are the possible ways for isolation. <br>
   * CursorConfig.DEFAULT : Default configuration used if null is passed to methods that create a cursor. <br>
   * CursorConfig.READ_COMMITTED : This ensures the stability of the current data item read by the cursor but permits
   * data read by this cursor to be modified or deleted prior to the commit of the transaction. <br>
   * CursorConfig.READ_UNCOMMITTED : A convenience instance to configure read operations performed by the cursor to
   * return modified but not yet committed data. <br>
   * <p>
   * During our testing we found that READ_UNCOMMITTED does not raise any problem and gives a performance enhancement
   * over READ_COMMITTED. Since we never read the map which has been marked for deletion by the DGC the deadlocks are
   * avoided
   * 
   * @return number of entries in Maps database deleted, if less than DELETE_BATCH_SIZE, then there could be more
   *         entries for the same map ID.
   */
  private int markForDeletion(final ObjectID id, final PersistenceTransaction tx) {
    int mapEntriesDeleted = 0;
    final byte idb[] = Conversion.long2Bytes(id.toLong());
    final TCDatabaseEntry entry = new TCDatabaseEntry();
    entry.setKey(idb);
    final TCDatabaseCursor<byte[], byte[]> cursor = this.database.openCursor(tx, id.toLong());
    try {
      while (mapEntriesDeleted <= DELETE_BATCH_SIZE && cursor.getNext(entry)) {
        cursor.delete();
        mapEntriesDeleted++;
      }
    } finally {
      cursor.close();
    }

    return mapEntriesDeleted;
  }
}
