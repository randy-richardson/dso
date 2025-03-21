/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.locks.LockID;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.MapOperationType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;

import java.util.Random;

public class MockModesAdd {

  private static final int BYTE_ARRAY_LENGTH = 32;
  private static Random    random            = new Random();

  static {
    random.setSeed(System.currentTimeMillis());
  }

  // final mappings
  // key -> (id, mapOid, valueOid)
  // oid -> value
  public static void addStrongValueToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                           LockID lockID, MockSerializedEntry value, ObjectID mapID,
                                           MapOperationType operationType) {
    addStrongValueToCacheWithAwardID(cache, store, key, lockID, value, mapID, operationType, 1L);
  }

  public static void addStrongValueToCacheWithAwardID(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                                      LockID lockID, MockSerializedEntry value, ObjectID mapID,
                                                      MapOperationType operationType, long awardId) {
    ObjectID valueObjectID = value != null ? value.getObjectID() : ObjectID.NULL_ID;
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreStrongValue(lockID, value, valueObjectID, awardId);
    addToCache(cache, store, key, value, operationType, localStoreValue);
  }

  // final mappings
  // key -> (id, mapOid, value)
  // valueOid -> List<key>
  public static void addEventualValueToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                             MockSerializedEntry value, ObjectID mapID, MapOperationType operationType) {
    ObjectID valueID = value != null ? value.getObjectID() : ObjectID.NULL_ID;
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreEventualValue(valueID, value);
    addToCache(cache, store, key, value, operationType, localStoreValue);
  }

  // final mappings
  // key -> (null, mapOid, valueOid)
  // oid -> value
  public static void addIncoherentValueToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                               MockSerializedEntry value, ObjectID mapID, MapOperationType operationType) {
    AbstractLocalCacheStoreValue localStoreValue = new LocalCacheStoreEventualValue(value.getObjectID(), value);
    addToCache(cache, store, key, value, operationType, localStoreValue);
  }

  private static void addToCache(ServerMapLocalCache cache, TCObjectSelfStore store, String key,
                                 MockSerializedEntry value, MapOperationType operationType,
                                 AbstractLocalCacheStoreValue localStoreValue) {
    store.addTCObjectSelf(cache.getInternalStore(), localStoreValue, value, true);
    cache.addToCache(key, localStoreValue, operationType);
  }

  public static MockSerializedEntry createMockSerializedEntry(int oid) {
    return createMockSerializedEntry(oid, getRandomlyFilledByteArray());
  }

  public static MockSerializedEntry createMockSerializedEntry(int oid, byte[] array) {
    MockSerializedEntry entry = new MockSerializedEntry(new ObjectID(oid), array, null);
    return entry;
  }

  private static byte[] getRandomlyFilledByteArray() {
    byte[] array = new byte[BYTE_ARRAY_LENGTH];
    random.nextBytes(array);
    return array;
  }

}
