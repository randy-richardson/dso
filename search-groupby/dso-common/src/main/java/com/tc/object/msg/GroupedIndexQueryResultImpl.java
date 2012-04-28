/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.search.GroupedQueryResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupedIndexQueryResultImpl extends IndexQueryResultImpl implements GroupedQueryResult {

  private List<NVPair> aggregatorResults;
  private List<NVPair> groupByAttributes;

  GroupedIndexQueryResultImpl() {
    //
  }

  public GroupedIndexQueryResultImpl(List<NVPair> attributes, List<NVPair> sortAttributes,
                                     List<NVPair> groupByAttributes, List<NVPair> aggregatorResults) {
    super(attributes, sortAttributes);
    this.groupByAttributes = groupByAttributes;
    this.aggregatorResults = aggregatorResults;
  }

  @Override
  public void serializeTo(TCByteBufferOutput output) {
    super.serializeTo(output);
    output.writeInt(this.groupByAttributes.size());
    for (NVPair pair : this.groupByAttributes) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }

    output.writeInt(this.aggregatorResults.size());
    for (NVPair pair : this.aggregatorResults) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }

  }

  @Override
  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    super.deserializeFrom(input);

    int size = input.readInt();

    this.groupByAttributes = new ArrayList<NVPair>(size);

    for (int i = 0; i < size; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.groupByAttributes.add(pair);
    }

    int aggregatorCount = input.readInt();
    this.aggregatorResults = new ArrayList<NVPair>(aggregatorCount);
    for (int i = 0; i < aggregatorCount; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.aggregatorResults.add(pair);
    }
    return this;
  }

  @Override
  public List<NVPair> getAggregatorResults() {
    return Collections.unmodifiableList(aggregatorResults);
  }

  @Override
  public List<NVPair> getGroupedAttributes() {
    return Collections.unmodifiableList(groupByAttributes);
  }

}
