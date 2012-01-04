/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config.schema;

import com.tc.config.schema.Config;

/**
 * Represents the per-application config for DSO.
 */
public interface DSOApplicationConfig extends Config {

  InstrumentedClass[] instrumentedClasses();

  Lock[] locks();

  Root[] roots();

  boolean supportSharingThroughReflection();

}
