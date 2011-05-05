/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;

public class MemcacheElementDNA implements DNAInternal {

  private final ObjectID oid;
  private final long     version;
  private final byte[]   value;

  public MemcacheElementDNA(ObjectID oid, long version, byte[] value) {
    this.oid = oid;
    this.version = version;
    this.value = value;
  }

  public int getArraySize() {
    return 0;
  }

  public DNACursor getCursor() {
    return new MemcacheElementDNACursor();
  }

  public String getDefiningLoaderDescription() {
    return null;
  }

  public ObjectID getObjectID() throws DNAException {
    return oid;
  }

  public ObjectID getParentObjectID() throws DNAException {
    return ObjectID.NULL_ID;
  }

  public String getTypeName() {
    return "org.terracotta.cache.serialization.SerializedEntry";
  }

  public long getVersion() {
    return this.version;
  }

  public boolean hasLength() {
    return false;
  }

  public boolean isDelta() {
    return false;
  }

  public MetaDataReader getMetaDataReader() {
    return null;
  }

  public boolean hasMetaData() {
    return false;
  }

  private class MemcacheElementDNACursor implements DNACursor {

    PhysicalAction physicalAction;
    boolean        next = true;

    public Object getAction() {
      return physicalAction;
    }

    public int getActionCount() {
      return 1;
    }

    public LogicalAction getLogicalAction() {
      throw new ImplementMe();
    }

    public PhysicalAction getPhysicalAction() {
      return physicalAction;
    }

    public boolean next() throws IOException {
      if (next) {
        next = false;
        physicalAction = new PhysicalAction(value);
        return true;
      }
      return next;
    }

    public boolean next(DNAEncoding arg0) throws IOException, ClassNotFoundException {
      throw new ImplementMe();
    }

    public void reset() throws UnsupportedOperationException {
      throw new ImplementMe();

    }

  }
}
