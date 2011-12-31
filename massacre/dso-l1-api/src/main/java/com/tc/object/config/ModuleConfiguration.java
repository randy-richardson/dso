/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

public interface ModuleConfiguration {

  void addWriteAutolock(String expression);

  void addIncludePattern(String expression, boolean honorTransient);

  void addIncludePattern(String expression);

  void addDelegateMethodAdapter(String type, String delegateType, String delegateField);

}
