/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import java.util.HashMap;
import java.util.Map;

/**
 * this interface is to collect all tokens and update these content.
 */
public class TickerToken implements TCSerializable {

  protected final Map<Integer, Boolean> tokenStateMap;
  protected final int                   id;

  public TickerToken(int id) {
    this(id, new HashMap<Integer, Boolean>());
  }

  public TickerToken(int id, Map<Integer, Boolean> tokenStateMap) {
    this.id = id;
    this.tokenStateMap = tokenStateMap;
  }

  public int getID() {
    return id;
  }

  public void collectToken(int aId, boolean dirtyState) {
    tokenStateMap.put(aId, dirtyState);
  }

  public Map<Integer, Boolean> getTokenStateMap() {
    return tokenStateMap;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) {

    return null;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    //
  }

}
