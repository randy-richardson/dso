/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.invalidation.Invalidations;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.util.ObjectIDSet;

import java.util.Map;

public interface InvalidateObjectManager {

  public void invalidateObjectFor(ClientID clientID, Invalidations invalidations);

  public Invalidations getObjectsIDsToInvalidate(ClientID clientID);

  public void addObjectsToValidateFor(ClientID clientID, Map<ObjectID, ObjectIDSet> objectIDsToValidate);

  public void start();

  public void validateObjects(ObjectIDSet validEntries);

}
