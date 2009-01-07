/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.impl;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.cache.NullCache;
import com.tc.objectserver.api.TestSink;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.dgc.impl.FullGCHook;
import com.tc.objectserver.dgc.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.impl.InMemoryManagedObjectStore;
import com.tc.objectserver.impl.ObjectManagerConfig;
import com.tc.objectserver.impl.ObjectManagerImpl;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.objectserver.persistence.impl.TestPersistenceTransactionProvider;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

public class MarkAndSweepGCBenchMarkTest extends TestCase {
  private MarkAndSweepGarbageCollector collector;
  private ObjectManagerImpl            objectManager;
  private ClientStateManager           stateManager;

  protected void setUp() {
    this.collector = new MarkAndSweepGarbageCollector(new ObjectManagerConfig(300000, true, true, true, true, 60000));
    this.stateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class));
    this.objectManager = new ObjectManagerImpl(new TestObjectManagerConfig(0, false),
                                               new TCThreadGroup(new ThrowableHandler(TCLogging
                                                   .getLogger(ObjectManagerImpl.class))), stateManager,
                                               new InMemoryManagedObjectStore(new HashMap()), new NullCache(),
                                               new TestPersistenceTransactionProvider(), new TestSink(),
                                               new TestSink(), new ObjectStatsRecorder());
    
    this.objectManager.setGarbageCollector(collector);
  }

  public void testGC() {
    ObjectID root1 = new ObjectID(11111123, 0);
    objectManager.createRoot("root1", root1);
    createObjects(createObjectIDSet(11111123, 11111124));

    Set<ObjectID> parent = createObjectIDSet(0, 2000000);
    createObjects(parent);

    TestManagedObject obj = getObjectByID(new ObjectID(11111123, 0));
    obj.addReferences(parent);

    Set<ObjectID> children1 = createObjectIDSet(5000000, 6000000);
    createObjects(children1);

    obj = getObjectByID(new ObjectID(5, 0));
    obj.addReferences(children1);

    Set<ObjectID> children2 = createObjectIDSet(8000000, 9000000);
    createObjects(children2);

    obj = getObjectByID(new ObjectID(4, 0));
    obj.addReferences(children2);

    createObjects(createObjectIDSet(3000000, 4000000));

    objectManager.start();

    Assert.assertEquals(objectManager.getAllObjectIDs().size(), 5000001);
    System.out.println("GC started");
    collector.start();
    collector.doGC(new FullGCHook(collector, objectManager, stateManager));
    Assert.assertEquals(objectManager.getAllObjectIDs().size(), 4000001);

    obj = getObjectByID(new ObjectID(5, 0));
    obj.removeReferences(children1);

    collector.doGC(new FullGCHook(collector, objectManager, stateManager));
    Assert.assertEquals(objectManager.getAllObjectIDs().size(), 3000001);

    obj = getObjectByID(new ObjectID(4, 0));
    obj.removeReferences(children2);

    collector.doGC(new FullGCHook(collector, objectManager, stateManager));
    Assert.assertEquals(objectManager.getAllObjectIDs().size(), 2000001);
  }

  private TestManagedObject getObjectByID(ObjectID id) {
    ManagedObject obj = objectManager.getObjectByIDOrNull(id);
    if (obj != null) {
      objectManager.releaseReadOnly(obj);
      return (TestManagedObject) obj;
    }
    throw new AssertionError(id + " could not be found");
  }

  private void createObjects(Set<ObjectID> ids) {
    createObjects(ids, new HashSet<ObjectID>());
  }

  private void createObjects(Set<ObjectID> ids, Set<ObjectID> children) {
    for (Iterator<ObjectID> iter = ids.iterator(); iter.hasNext();) {
      ObjectID id = iter.next();
      TestManagedObject mo = new TestManagedObject(id, new ArrayList<ObjectID>(children));
      objectManager.createObject(mo);
      objectManager.getObjectStore().addNewObject(mo);
    }
  }

  private Set<ObjectID> createObjectIDSet(int startIndex, int endIndex) {
    Set<ObjectID> oidSet = new HashSet<ObjectID>();
    for (int i = startIndex; i < endIndex; i++) {
      oidSet.add(new ObjectID(i, 0));
    }
    return oidSet;
  }

  private static class TestObjectManagerConfig extends ObjectManagerConfig {

    public long    myGCThreadSleepTime = 100;
    public boolean paranoid;

    public TestObjectManagerConfig() {
      super(10000, true, true, true, false, 60000);
    }

    TestObjectManagerConfig(long gcThreadSleepTime, boolean doGC) {
      super(gcThreadSleepTime, doGC, true, true, false, 60000);
    }

    @Override
    public long gcThreadSleepTime() {
      return myGCThreadSleepTime;
    }

    @Override
    public boolean paranoid() {
      return paranoid;
    }
  }
}
