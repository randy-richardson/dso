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
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.util.List;

public class SearchPutIfAbsentContext extends BaseSearchEventContext {
  private final List<NVPair> attributes;
  private final String       cacheKey;
  private final ValueID      cacheValue;

  public SearchPutIfAbsentContext(ObjectID segmentOid, String name, String cacheKey, ValueID cacheValue,
                                  List<NVPair> attributes, MetaDataProcessingContext metaDataContext) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.attributes = attributes;
  }

  public List<NVPair> getAttributes() {
    return attributes;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public ValueID getCacheValue() {
    return cacheValue;
  }

}
