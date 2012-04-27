/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.ObjectID;
import com.tc.object.metadata.NVPair;
import com.tc.search.NonGroupedQueryResult;

import java.io.IOException;
import java.util.List;

public class NonGroupedIndexQueryResultImpl extends IndexQueryResultImpl implements NonGroupedQueryResult, Comparable {

  private String   key;
  private ObjectID valueOID = ObjectID.NULL_ID;

  NonGroupedIndexQueryResultImpl() {
    //
  }

  public NonGroupedIndexQueryResultImpl(String key, ObjectID valueOID, List<NVPair> attributes,
                                        List<NVPair> sortAttributes) {
    super(attributes, sortAttributes);
    this.key = key;
    this.valueOID = valueOID;
  }

  /**
   * {@inheritDoc}
   */
  public String getKey() {
    return this.key;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  public ObjectID getValue() {
    return valueOID;
  }

  @Override
  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    this.key = input.readString();
    this.valueOID = new ObjectID(input.readLong());
    super.deserializeFrom(input);
    return this;
  }

  public void serializeTo(TCByteBufferOutput output) {
    output.writeString(this.key);
    output.writeLong(this.valueOID.toLong());
    super.serializeTo(output);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NonGroupedIndexQueryResultImpl other = (NonGroupedIndexQueryResultImpl) obj;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    return true;
  }

  public int compareTo(Object o) {
    if (this == o) return 0;
    if (o == null) return -1;
    if (getClass() != o.getClass()) return -1;
    NonGroupedIndexQueryResultImpl other = (NonGroupedIndexQueryResultImpl) o;
    if (key == null) {
      if (other.key != null) return -1;
    }
    return other.key.compareTo(key);
  }

  @Override
  public String toString() {
    return new StringBuilder(256).append("<").append(getClass().getSimpleName()).append(": key=").append(key)
        .append(" value=").append(valueOID).append(" attributes=").append(getAttributes()).append(" sortAttributes=")
        .append(getSortAttributes()).append(">").toString();
  }

}
