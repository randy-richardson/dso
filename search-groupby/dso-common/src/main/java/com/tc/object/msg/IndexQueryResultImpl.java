/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.NullObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class IndexQueryResultImpl implements IndexQueryResult {

  protected static final ObjectStringSerializer NULL_SERIALIZER = new NullObjectStringSerializer();

  private List<NVPair>                          attributes;
  private List<NVPair>                          sortAttributes;

  protected IndexQueryResultImpl() {
    //
  }

  protected IndexQueryResultImpl(List<NVPair> attributes, List<NVPair> sortAttributes) {
    this.attributes = attributes;
    this.sortAttributes = sortAttributes;
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    int size = input.readInt();

    this.attributes = size > 0 ? new ArrayList<NVPair>() : Collections.EMPTY_LIST;

    for (int i = 0; i < size; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.attributes.add(pair);
    }

    int sortSize = input.readInt();
    this.sortAttributes = sortSize > 0 ? new ArrayList<NVPair>() : Collections.EMPTY_LIST;

    for (int i = 0; i < sortSize; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.sortAttributes.add(pair);
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeInt(this.attributes.size());
    for (NVPair pair : this.attributes) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }

    output.writeInt(this.sortAttributes.size());
    for (NVPair pair : this.sortAttributes) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }

  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getAttributes() {
    return Collections.unmodifiableList(this.attributes);
  }

  /**
   * {@inheritDoc}
   */
  public List<NVPair> getSortAttributes() {
    return Collections.unmodifiableList(this.sortAttributes);
  }

  static IndexQueryResult getInstance(boolean isGrouped) {
    return isGrouped ? new NonGroupedIndexQueryResultImpl() : new GroupedIndexQueryResultImpl();
  }

}
