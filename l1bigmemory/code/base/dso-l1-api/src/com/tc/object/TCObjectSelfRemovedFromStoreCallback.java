/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public interface TCObjectSelfRemovedFromStoreCallback {

  /**
   * Called when the tcObjectSelf has been removed from the TCObjectSelfStore
   */
  void removedTCObjectSelfFromStore(TCObjectSelf tcoObjectSelf);
}
