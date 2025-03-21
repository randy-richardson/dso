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
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;

public class EntryForKeyResponseContext implements EventContext {

  private final ManagedObject mo;
  private final ObjectID      mapID;

  public EntryForKeyResponseContext(final ManagedObject mo, final ObjectID mapID) {
    this.mo = mo;
    this.mapID = mapID;
  }

  public ManagedObject getManagedObject() {
    return this.mo;
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  @Override
  public String toString() {
    return "EntryForKeyResponseContext [ map : " + this.mapID + "]";
  }
}
