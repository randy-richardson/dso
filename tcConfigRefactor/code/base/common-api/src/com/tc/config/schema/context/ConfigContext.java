/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.context;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.config.schema.OffHeapConfigItem;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.FileConfigItem;
import com.tc.config.schema.dynamic.StringArrayConfigItem;
import com.tc.config.schema.repository.BeanRepository;
import com.terracottatech.config.Offheap;

/**
 * Binds together a {@link BeanRepository} and a {@link DefaultValueProvider}, and provides convenience methods for
 * creating various items.
 */
public interface ConfigContext {

  void ensureRepositoryProvides(Class theClass);

  boolean hasDefaultFor(String xpath) throws XmlException;

  XmlObject defaultFor(String xpath) throws XmlException;

  boolean isOptional(String xpath) throws XmlException;

  IllegalConfigurationChangeHandler illegalConfigurationChangeHandler();

  XmlObject bean();

  Object syncLockForBean();

  void itemCreated(ConfigItem item);

  StringArrayConfigItem stringArrayItem(String xpath);

  FileConfigItem fileItem(String xpath);

  FileConfigItem substitutedFileItem(String xpath);

  FileConfigItem configRelativeSubstitutedFileItem(String xpath);

  OffHeapConfigItem offHeapConfigItem(String string, Offheap offheap);

}