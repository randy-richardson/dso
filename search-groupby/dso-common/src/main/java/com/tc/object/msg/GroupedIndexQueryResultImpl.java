/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.metadata.AbstractNVPair;
import com.tc.object.metadata.NVPair;
import com.tc.search.GroupedQueryResult;
import com.tc.search.aggregator.AbstractAggregator;
import com.tc.search.aggregator.Aggregator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupedIndexQueryResultImpl extends IndexQueryResultImpl implements GroupedQueryResult {

  private List<Aggregator> aggregators;
  private Set<NVPair>      groupByAttributes;

  GroupedIndexQueryResultImpl() {
    //
  }

  public GroupedIndexQueryResultImpl(List<NVPair> attributes, List<NVPair> sortAttributes,
                                     Set<NVPair> groupByAttributes, List<Aggregator> aggregatorResults) {
    super(attributes, sortAttributes);
    this.groupByAttributes = groupByAttributes;
    this.aggregators = aggregatorResults;
  }

  @Override
  public void serializeTo(TCByteBufferOutput output) {
    super.serializeTo(output);
    output.writeInt(this.groupByAttributes.size());
    for (NVPair pair : this.groupByAttributes) {
      pair.serializeTo(output, NULL_SERIALIZER);
    }

    output.writeInt(this.aggregators.size());
    for (Aggregator agg : this.aggregators) {
      agg.serializeTo(output);
    }

  }

  @Override
  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    super.deserializeFrom(input);

    int size = input.readInt();

    this.groupByAttributes = new HashSet<NVPair>(size);

    for (int i = 0; i < size; i++) {
      NVPair pair = AbstractNVPair.deserializeInstance(input, NULL_SERIALIZER);
      this.groupByAttributes.add(pair);
    }

    int aggregatorCount = input.readInt();
    this.aggregators = new ArrayList<Aggregator>(aggregatorCount);
    for (int i = 0; i < aggregatorCount; i++) {
      Aggregator aggregator = AbstractAggregator.deserializeInstance(input);
      this.aggregators.add(aggregator);
    }
    return this;
  }

  @Override
  public List<Aggregator> getAggregators() {
    return aggregators;
  }

  @Override
  public Set<NVPair> getGroupedAttributes() {
    return Collections.unmodifiableSet(groupByAttributes);
  }

}
