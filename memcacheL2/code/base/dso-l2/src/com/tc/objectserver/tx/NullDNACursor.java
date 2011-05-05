/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;

import java.io.IOException;

public class NullDNACursor implements DNACursor {

  public Object getAction() {
    return null;
  }

  public int getActionCount() {
    return 0;
  }

  public LogicalAction getLogicalAction() {
    return null;
  }

  public PhysicalAction getPhysicalAction() {
    return null;
  }

  public boolean next() throws IOException {
    return false;
  }

  public boolean next(DNAEncoding arg0) throws IOException, ClassNotFoundException {
    return false;
  }

  public void reset() throws UnsupportedOperationException {
    //

  }

}
