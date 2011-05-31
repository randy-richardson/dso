/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.io.IOException;

public class ObjectIDSetSerializer implements TCSerializable {
  private final ObjectIDSet oidSet;

  public ObjectIDSetSerializer() {
    this(new ObjectIDSet());
  }

  public ObjectIDSetSerializer(ObjectIDSet oidSet) {
    this.oidSet = oidSet;
  }

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    int size = in.readInt();
    for (int i = 0; i < size; i++) {
      oidSet.add(new ObjectID(in.readLong()));
    }
    return this.oidSet;
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeInt(oidSet.size());
    for (ObjectID oid : oidSet) {
      out.writeLong(oid.toLong());
    }
  }

  public ObjectIDSet getObjectIDSet() {
    return this.oidSet;
  }
}
