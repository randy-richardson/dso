/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.api;

import com.tc.invalidation.Invalidations;
import com.tc.net.ClientID;
import com.tc.util.ObjectIDSet;

import java.util.Set;

public interface InvalidateObjectManager {

  public void invalidateObjectFor(ClientID clientID, Invalidations invalidations);

  public Invalidations getObjectsIDsToInvalidate(ClientID clientID);

  public void addObjectsToValidateFor(ClientID clientID, Set objectIDsToValidate);

  public void start();

  public void validateObjects(ObjectIDSet validEntries);

}
