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
import com.tc.objectserver.persistence.TCMapsDatabase;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistentCollectionsUtil;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor.SleepycatPersistorBase;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;

public class SleepycatCollectionsPersistor extends SleepycatPersistorBase {

  private final TCMapsDatabase             database;
  private final BasicSerializer            serializer;
  private final ByteArrayOutputStream      bao;
  private final SleepycatCollectionFactory collectionFactory;
  private final TCObjectOutputStream       oo;

  public SleepycatCollectionsPersistor(TCLogger logger, TCMapsDatabase mapsDatabase,
                                       SleepycatCollectionFactory sleepycatCollectionFactory) {
    this.database = mapsDatabase;
    this.collectionFactory = sleepycatCollectionFactory;
    DSOSerializerPolicy policy = new DSOSerializerPolicy();
    this.serializer = new BasicSerializer(policy);
    this.bao = new ByteArrayOutputStream(1024);
    this.oo = new TCObjectOutputStream(bao);
  }

  public int saveCollections(PersistenceTransaction tx, ManagedObjectState state) throws IOException,
      TCDatabaseException {
    PersistableObjectState persistabeState = (PersistableObjectState) state;
    PersistableCollection collection = persistabeState.getPersistentCollection();
    return collection.commit(this, tx, database);
  }

  public synchronized byte[] serialize(long id, Object o) throws IOException {
    oo.writeLong(id);
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public synchronized byte[] serialize(Object o) throws IOException {
    serializer.serializeTo(o, oo);
    oo.flush();
    byte b[] = bao.toByteArray();
    bao.reset();
    return b;
  }

  public void loadCollectionsToManagedState(PersistenceTransaction tx, ObjectID id, ManagedObjectState state)
      throws IOException, ClassNotFoundException, TCDatabaseException {
    Assert.assertTrue(PersistentCollectionsUtil.isPersistableCollectionType(state.getType()));

    PersistableObjectState persistableState = (PersistableObjectState) state;
    Assert.assertNull(persistableState.getPersistentCollection());
    PersistableCollection collection = PersistentCollectionsUtil.createPersistableCollection(id, collectionFactory,
                                                                                             state.getType());
    collection.load(this, tx, database);
    persistableState.setPersistentCollection(collection);
  }

  public Object deserialize(int start, byte[] data) throws IOException, ClassNotFoundException {
    if (start >= data.length) return null;
    ByteArrayInputStream bai = new ByteArrayInputStream(data, start, data.length - start);
    ObjectInput ois = new TCObjectInputStream(bai);
    return serializer.deserializeFrom(ois);
  }

  public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    return deserialize(0, data);
  }

  /**
   * This method is slightly dubious in that it assumes that the ObjectID is the first 8 bytes of the Key in the entire
   * collections database.(which is true, but that logic is spread elsewhere)
   * 
   * @throws TCDatabaseException
   */
  public boolean deleteCollection(PersistenceTransaction tx, ObjectID id) throws TCDatabaseException {
    // XXX:: Since we read in one direction and since we have to read the first record of the next map to break out, we
    // need this to avoid deadlocks between commit thread and DGC thread. Hence READ_COMMITTED
    int written = database.deleteCollection(id.toLong(), tx);
    if (written > 0) { return true; }
    return false;
  }
}
