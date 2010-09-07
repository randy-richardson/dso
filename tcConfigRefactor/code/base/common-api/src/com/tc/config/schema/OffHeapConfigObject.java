/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.dynamic.XPathBasedConfigItem;
import com.terracottatech.config.Offheap;

public class OffHeapConfigObject extends XPathBasedConfigItem implements OffHeapConfigItem {

  public OffHeapConfigObject(ConfigContext context, String xPath, Offheap defaultOffHeap) {
    super(context, xPath, defaultOffHeap);
  }

  @Override
  protected Object fetchDataFromXmlObject(XmlObject xmlObject) {
    Boolean enabled = (Boolean) super.fetchDataFromXmlObjectByReflection(xmlObject, "getEnabled");
    if (enabled == null) { return null; }

    String maxDataSize = (String) super.fetchDataFromXmlObjectByReflection(xmlObject, "getMaxDataSize");
    if (maxDataSize == null) { return null; }

    Offheap offheap = Offheap.Factory.newInstance();
    offheap.setEnabled(enabled);
    offheap.setMaxDataSize(maxDataSize);
    return offheap;
  }

  public String getMaxDataSize() {
    return ((Offheap) getObject()).getMaxDataSize();
  }

  public boolean isEnabled() {
    return ((Offheap) getObject()).getEnabled();
  }

  @Override
  public String toString() {
    return ("OffHeapConfigObject : Enabled : " + isEnabled() + " Max data size : " + getMaxDataSize());
  }
}
