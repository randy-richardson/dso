/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.loaders.ClassProvider;

public class MockClassProvider implements ClassProvider {

  public MockClassProvider() {
    super();
  }

  public Class getClassFor(String className) throws ClassNotFoundException {
    return getClass().getClassLoader().loadClass(className);
  }

}
