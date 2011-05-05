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
import com.tc.object.metadata.MetaDataDescriptorInternal;

import java.util.Collections;
import java.util.Iterator;

public class MemcacheDNA implements DNAInternal {

  private final ObjectID oid;
  private final long     version;
  private final Object[] params;
  private final int      method;

  public MemcacheDNA(ObjectID oid, long version, Object[] params, int method) {
    this.oid = oid;
    this.version = version;
    this.params = params;
    this.method = method;
  }

  public int getArraySize() {
    return 0;
  }

  public DNACursor getCursor() {
    return new MemcacheDNACursor();
  }

  public String getDefiningLoaderDescription() {
    return null;
  }

  public ObjectID getObjectID() throws DNAException {
    return oid;
  }

  public ObjectID getParentObjectID() throws DNAException {
    return null;
  }

  public String getTypeName() {
    return null;
  }

  public long getVersion() {
    return version;
  }

  public boolean hasLength() {
    return false;
  }

  public boolean isDelta() {
    return false;
  }

  public MetaDataReader getMetaDataReader() {
    return new NullMetaDataReader();
  }

  public boolean hasMetaData() {
    return false;
  }

  private static class NullMetaDataReader implements MetaDataReader {

    public Iterator<MetaDataDescriptorInternal> iterator() {
      return Collections.EMPTY_LIST.iterator();
    }

  }

  private class MemcacheDNACursor implements DNACursor {

    boolean next;

    public MemcacheDNACursor() {
      next = true;
    }

    public int getActionCount() {
      return 1;
    }

    public boolean next() {
      if (next) {
        next = false;
      }
      return next;
    }

    public boolean next(DNAEncoding encoding) {
      throw new ImplementMe();
    }

    public void reset() throws UnsupportedOperationException {
      //
    }

    public LogicalAction getLogicalAction() {
      if (next) {
        return new LogicalAction(method, params);
      } else {
        throw new ImplementMe();
      }
    }

    public PhysicalAction getPhysicalAction() {
      throw new ImplementMe();
    }

    public Object getAction() {
      return null;
    }

  }
}
