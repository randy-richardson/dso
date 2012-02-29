/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class BaseSearchEventContext implements SearchEventContext, MultiThreadedEventContext {

  private static final int                INDEX_PER_CACHE     = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(TCPropertiesConsts.SEARCH_LUCENE_INDEXES_PER_CACHE);
  private static final int                SEDA_SEARCH_THREADS = TCPropertiesImpl.getProperties()
                                                                  .getInt(TCPropertiesConsts.L2_SEDA_SEARCH_THREADS);

  private final MetaDataProcessingContext metaDataContext;
  private final String                    cacheName;
  private final ObjectID                  segmentOid;

  private int                             hashCode;

  private final static class StageQFreq {
    private final int qId;
    private int       occurCt = 0;

    private StageQFreq(int bucket) {
      qId = bucket;
    }
  }

  private static final ConcurrentMap<BaseSearchEventContext, Integer> buckets = new ConcurrentHashMap<BaseSearchEventContext, Integer>(
                                                                                                                                       SEDA_SEARCH_THREADS);
  private static final PriorityQueue<StageQFreq>                      tPool   = new PriorityQueue<StageQFreq>(
                                                                                                              SEDA_SEARCH_THREADS,
                                                                                                              new Comparator<StageQFreq>() {
                                                                                                                public int compare(StageQFreq one,
                                                                                                                                   StageQFreq other) {
                                                                                                                  return one.occurCt < other.occurCt ? -1
                                                                                                                      : (one.occurCt == other.occurCt ? 0
                                                                                                                          : 1);
                                                                                                                }
                                                                                                              });
  static {
    for (int i = 0; i < SEDA_SEARCH_THREADS; i++) {
      tPool.add(new StageQFreq(i));
    }
  }

  public BaseSearchEventContext(ObjectID segmentOid, String cacheName, MetaDataProcessingContext metaDataContext) {
    this.segmentOid = segmentOid;
    this.cacheName = cacheName;
    this.metaDataContext = metaDataContext;

    computeHashCode();
  }

  public final Object getKey() {

    Integer dest = buckets.get(this); // careful with autoboxing!
    if (dest == null) {

      StageQFreq lowest = tPool.peek();
      dest = lowest.qId;
      Integer tmp = buckets.putIfAbsent(this, dest);
      if (tmp == null) {
        tPool.poll();
        lowest.occurCt++;
        assert tPool.size() < SEDA_SEARCH_THREADS;
        tPool.add(lowest);
      } else {
        dest = tmp;
      }
    }

    return dest;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BaseSearchEventContext)) return false;
    BaseSearchEventContext other = (BaseSearchEventContext) obj;
    return hashCode == other.hashCode;
  }

  public MetaDataProcessingContext getMetaDataProcessingContext() {
    return metaDataContext;
  }

  public String getCacheName() {
    return cacheName;
  }

  public ObjectID getSegmentOid() {
    return segmentOid;
  }

  private void computeHashCode() {
    // Pick the start thread index using cache name
    int threadStart = Math.abs(cacheName.hashCode()) % SEDA_SEARCH_THREADS;
    // Pick the next n threads for n indexes using segment id
    int indexSegment = (int) (Math.abs(segmentOid.toLong()) % INDEX_PER_CACHE);
    hashCode = (threadStart + indexSegment) % SEDA_SEARCH_THREADS;

  }
}
