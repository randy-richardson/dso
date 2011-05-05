/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAException;
import com.tc.object.dna.api.DNAInternal;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.metadata.MetaDataDescriptorInternal;

import java.util.Collections;
import java.util.Iterator;

public class MemcacheRootDNA implements DNAInternal {

  private final ObjectID oid;

  public MemcacheRootDNA(ObjectID oid) {
    this.oid = oid;
  }

  public int getArraySize() {
    return 0;
  }

  public DNACursor getCursor() {
    return new NullDNACursor();
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
    return "com.terracotta.toolkit.collections.ConcurrentDistributedServerMapDso";
  }

  public long getVersion() {
    return 0;
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

}
