/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.GroupedQueryResult;
import com.tc.search.IndexQueryResult;
import com.tc.search.aggregator.Aggregator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResultTools {
  public static void mergeGroupedResults(List<? extends IndexQueryResult> indexResults) {
    List<GroupedQueryResult> groups = (List<GroupedQueryResult>) indexResults;
    Map<Set<NVPair>, GroupedQueryResult> uniqueGroups = new HashMap<Set<NVPair>, GroupedQueryResult>();

    for (GroupedQueryResult group : groups) {
      Set<NVPair> groupBy = group.getGroupedAttributes();

      GroupedQueryResult dest = uniqueGroups.get(groupBy);
      if (dest == null) uniqueGroups.put(groupBy, group);
      else {
        aggregate(dest.getAggregators(), group.getAggregators());
      }
    }

    groups.clear();
    groups.addAll(uniqueGroups.values());
  }

  public static void aggregate(List<Aggregator> aggregates, List<Aggregator> incoming) {
    if (incoming.isEmpty()) {
      return;
    } else {
      if (aggregates.isEmpty()) {
        aggregates.addAll(incoming);
      } else {
        for (int i = 0; i < aggregates.size(); i++) {
          aggregates.get(i).accept(incoming.get(i));
        }
      }
    }
  }

}
