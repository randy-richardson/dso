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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

class BaseSearchEventContext implements SearchEventContext, MultiThreadedEventContext {

  private static final int                INDEX_PER_CACHE     = TCPropertiesImpl
                                                                  .getProperties()
                                                                  .getInt(TCPropertiesConsts.SEARCH_LUCENE_INDEXES_PER_CACHE);
  private static final int                SEDA_SEARCH_THREADS = TCPropertiesImpl.getProperties()
                                                                  .getInt(TCPropertiesConsts.L2_SEDA_SEARCH_THREADS);
  private static final BucketManager      mgr                 = new BucketManager();

  private final MetaDataProcessingContext metaDataContext;
  private final String                    cacheName;
  private final ObjectID                  segmentOid;

  private static final class BucketManager {
    private final static class StageQFreq {
      private final int qId;
      private int       occurCt = 0;

      private StageQFreq(int bucket) {
        qId = bucket;
      }
    }

    private final Map<String, Map<Integer, Integer>> bucketDist = new HashMap<String, Map<Integer, Integer>>();

    private final Map<Integer, Integer>              bucketFreq = new HashMap<Integer, Integer>(SEDA_SEARCH_THREADS);

    private final PriorityQueue<StageQFreq>          tPool      = new PriorityQueue<StageQFreq>(
                                                                                                SEDA_SEARCH_THREADS,
                                                                                                new Comparator<StageQFreq>() {
                                                                                                  public int compare(StageQFreq one,
                                                                                                                     StageQFreq other) {
                                                                                                    return one.occurCt < other.occurCt ? -1
                                                                                                        : (one.occurCt == other.occurCt ? 0
                                                                                                            : 1);
                                                                                                  }
                                                                                                });

    private BucketManager() {
      for (int i = 0; i < SEDA_SEARCH_THREADS; i++) {
        tPool.add(new StageQFreq(i));
        bucketFreq.put(i, 0);
      }
    }

    private Map<Integer, Integer> getCacheBuckets(String cacheName) {
      Map<Integer, Integer> hash = bucketDist.get(cacheName);
      if (hash == null) {
        hash = new HashMap<Integer, Integer>(INDEX_PER_CACHE);
        bucketDist.put(cacheName, hash);
      }
      return hash;
    }

    private int rehashIfNeeded(int bucket) {
      int occurs = bucketFreq.get(bucket);
      StageQFreq lowest = tPool.peek();

      // Not the least loaded bucket
      if (occurs++ > lowest.occurCt) {
        bucket = lowest.qId;
        occurs = ++lowest.occurCt;
        tPool.poll();
      } else {
        // find, increment frequency, and remove
        for (Iterator<StageQFreq> s = tPool.iterator(); s.hasNext();) {
          lowest = s.next();
          if (lowest.qId == bucket) {
            s.remove();
            break;
          }
        }
        assert lowest.qId == bucket;
        lowest.occurCt++;
      }

      // re-add to PQ; no longer the smallest element
      tPool.add(lowest);
      // record bucket frequency
      bucketFreq.put(bucket, occurs);
      return bucket;

    }
  }

  public BaseSearchEventContext(ObjectID segmentOid, String cacheName, MetaDataProcessingContext metaDataContext) {
    this.segmentOid = segmentOid;
    this.cacheName = cacheName;
    this.metaDataContext = metaDataContext;

  }

  public final Object getKey() {
    Map<Integer, Integer> idxBuckets = mgr.getCacheBuckets(cacheName);
    int idxOffset = getIndexOffset();
    Integer dest = idxBuckets.get(idxOffset);
    if (dest == null) {
      dest = computeCandidateBucket();
      Integer tmp = idxBuckets.put(idxOffset, dest);
      assert tmp == null;
    }
    return dest;
  }

  public static void main(String[] args) {
    Random rand = new Random();
    String cacheName[] = { "ehcacheperf_owners_0", "ehcacheperf_pets_2", "ehcacheperf_visits_1" };
    for (String cache : cacheName) {
      for (int i = 0; i < 50000; i++) {
        new BaseSearchEventContext(new ObjectID(rand.nextInt(256)), cache, null).getKey();
      }
    }

    for (Map.Entry<String, Map<Integer, Integer>> mapping : mgr.bucketDist.entrySet()) {
      for (Map.Entry<Integer, Integer> buckets : mapping.getValue().entrySet()) {
        System.out.println(mapping.getKey() + "/" + buckets.getKey() + " ==> " + buckets.getValue());
      }
    }

    for (Map.Entry<Integer, Integer> bf : mgr.bucketFreq.entrySet()) {
      System.out.println(bf.getKey() + " => " + bf.getValue());
    }

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

  private int computeCandidateBucket() {
    // Pick the start thread index using cache name
    int threadStart = Math.abs(cacheName.hashCode()) % SEDA_SEARCH_THREADS;
    // Pick the next n threads for n indexes using segment id
    int indexSegment = getIndexOffset();
    int b = (threadStart + indexSegment) % SEDA_SEARCH_THREADS;

    return mgr.rehashIfNeeded(b);

  }

  private int getIndexOffset() {
    return (int) (Math.abs(segmentOid.toLong()) % INDEX_PER_CACHE);
  }

}
