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

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Map;

public abstract class ServerMapRequestContext implements ObjectManagerResultsContext {

  private final static TCLogger logger    = TCLogging.getLogger(ServerMapRequestContext.class);

  private final ObjectIDSet     lookupIDs = new BitSetObjectIDSet();
  private final ObjectID        mapID;
  private final ClientID        clientID;
  private final Sink            destinationSink;

  public ServerMapRequestContext(final ClientID clientID, final ObjectID mapID, final Sink destinationSink) {
    this.clientID = clientID;
    this.mapID = mapID;
    this.destinationSink = destinationSink;
    this.lookupIDs.add(mapID);
  }

  public abstract ServerMapRequestType getRequestType();

  // override by who has it
  public abstract ServerMapRequestID getRequestID();

  public ClientID getClientID() {
    return this.clientID;
  }

  public ObjectID getServerTCMapID() {
    return this.mapID;
  }

  @Override
  public String toString() {
    return "RequestEntryForKeyContext [  mapID = " + this.mapID + " clientID : " + this.clientID + " requestType : "
           + getRequestType() + " requestID : " + getRequestID() + "]";
  }

  @Override
  public ObjectIDSet getLookupIDs() {
    return this.lookupIDs;
  }

  @Override
  public ObjectIDSet getNewObjectIDs() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  @Override
  public void setResults(final ObjectManagerLookupResults results) {
    final ObjectIDSet missingObjects = results.getMissingObjectIDs();

    if (!missingObjects.isEmpty()) {
      logger.error("Missing ObjectIDs : " + missingObjects + " Request Context : " + this);
      final ServerMapMissingObjectResponseContext responseContext = new ServerMapMissingObjectResponseContext(
                                                                                                              this.mapID);
      this.destinationSink.add(responseContext);
      return;
    }

    final Map<ObjectID, ManagedObject> objects = results.getObjects();

    if (objects.size() != 1) { throw new AssertionError("Asked for 1, got more or less"); }

    final ManagedObject mo = objects.get(this.mapID);

    if (mo == null) { throw new AssertionError("ServerMap (mapID " + this.mapID + ") is null "); }

    final EntryForKeyResponseContext responseContext = new EntryForKeyResponseContext(mo, this.mapID);
    this.destinationSink.add(responseContext);
  }
}