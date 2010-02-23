/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.je.DatabaseEntry;
import com.tc.objectserver.core.api.ManagedObject;

import java.util.HashMap;
import java.util.Map;

public class SleepycatSerializationAdapter implements SerializationAdapter {

  private final ClassCatalog classCatalog;
  private final Map          entryBindings;

  public SleepycatSerializationAdapter(ClassCatalog classCatalog) {
    this.classCatalog = classCatalog;
    this.entryBindings = new HashMap();
  }

  public byte[] serializeManagedObject(ManagedObject mo) {
    return serialize(mo, ManagedObject.class);
  }

  public byte[] serializeString(String string) {
    return serialize(string, String.class);
  }

  public ManagedObject deserializeManagedObject(byte[] entry) {
    return (ManagedObject) deserialize(entry, ManagedObject.class);
  }

  public String deserializeString(byte[] entry) {
    return (String) deserialize(entry, String.class);
  }

  public void reset() {
    return;
  }

  private byte[] serialize(Object o, Class clazz) {
    DatabaseEntry entry = new DatabaseEntry();
    getEntryBindingFor(clazz).objectToEntry(o, entry);
    return entry.getData();
  }

  private Object deserialize(byte[] data, Class clazz) {
    DatabaseEntry entry = new DatabaseEntry();
    entry.setData(data);
    return getEntryBindingFor(clazz).entryToObject(entry);
  }

  private EntryBinding getEntryBindingFor(Class c) {
    EntryBinding rv = (EntryBinding) this.entryBindings.get(c);
    if (rv == null) {
      rv = new SerialBinding(this.classCatalog, c);
      this.entryBindings.put(c, rv);
    }
    return rv;
  }

}
