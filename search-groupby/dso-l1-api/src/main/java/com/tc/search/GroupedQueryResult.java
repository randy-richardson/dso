/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.aggregator.Aggregator;

import java.util.List;
import java.util.Set;

public interface GroupedQueryResult extends IndexQueryResult {

  public List<Aggregator> getAggregators();

  public Set<NVPair> getGroupedAttributes();
}
