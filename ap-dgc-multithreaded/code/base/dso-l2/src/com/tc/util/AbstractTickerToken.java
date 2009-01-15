/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTickerToken implements TickerToken, TCSerializable {

  protected int                   primaryID;
  protected int                   primaryTickValue;
  protected Map<Integer, Boolean> tokenStateMap = new HashMap<Integer, Boolean>();

  public AbstractTickerToken(int primaryID, int primaryTickValue) {
    this.primaryID = primaryID;
    this.primaryTickValue = primaryTickValue;
  }

  public AbstractTickerToken(int primaryID, int primaryTickValue, Map<Integer, Boolean> tokenStateMap) {
    this(primaryID, primaryTickValue);
    this.tokenStateMap = tokenStateMap;
  }

  public int getPrimaryID() {
    return primaryID;
  }

  public int getPrimaryTickValue() {
    return primaryTickValue;
  }

  public void collectToken(int aId, boolean dirtyState) {
    tokenStateMap.put(aId, dirtyState);
  }

  public Map<Integer, Boolean> getTokenStateMap() {
    return tokenStateMap;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) {
    try {
      primaryID = serialInput.readInt();
      primaryTickValue = serialInput.readInt();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    tokenStateMap = (HashMap<Integer, Boolean>)deserializeObject(serialInput);
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(primaryID);
    serialOutput.writeInt(primaryTickValue);
    serializeObject(serialOutput, tokenStateMap);
  }
  
  protected void serializeObject(TCByteBufferOutput serialOutput, Object obj) {
    ByteArrayOutputStream boas = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oos = new ObjectOutputStream(boas);
      oos.writeObject(obj);
      oos.close();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    byte[] objectArray = boas.toByteArray();
    serialOutput.writeInt(objectArray.length);
    serialOutput.write(objectArray);
  }

  protected Object deserializeObject(TCByteBufferInput serialInput) {
    Object obj = null;
    try {
      int objectArrayLength = serialInput.readInt();
      byte[] objectArray = new byte[objectArrayLength];
      serialInput.read(objectArray);
      ByteArrayInputStream bais = new ByteArrayInputStream(objectArray);
      ObjectInputStream ois = new ObjectInputStream(bais);
      obj = ois.readObject();
    } catch (ClassNotFoundException e) {
      throw new AssertionError(e);
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    return obj;
  }

}
