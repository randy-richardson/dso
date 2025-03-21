/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tc.objectserver.gtx;

import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.State;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GlobalTransactionDescriptor {

  private static final State                              INIT            = new State("INIT");
  private static final State                              APPLY_INITIATED = new State("APPLY_INITIATED");
  private static final State                              COMMIT_COMPLETE = new State("COMMIT_COMPLETE");

  private final ServerTransactionID                       stxn;
  private final GlobalTransactionID                       gid;
  private volatile State                                  state;
  private volatile Map<LogicalChangeID, LogicalChangeResult> changeResults   = null;

  public GlobalTransactionDescriptor(ServerTransactionID serverTransactionID, GlobalTransactionID gid) {
    this.stxn = serverTransactionID;
    this.gid = gid;
    this.state = INIT;
  }

  public void saveStateFrom(GlobalTransactionDescriptor old) {
    this.state = old.state;
  }

  public void commitComplete() {
    if (this.state == COMMIT_COMPLETE) { throw new AssertionError("Already commited : " + this + " state = " + state); }
    this.state = COMMIT_COMPLETE;
  }

  public boolean initiateApply() {
    boolean toInitiate = (this.state == INIT);
    if (toInitiate) {
      this.state = APPLY_INITIATED;
    }
    return toInitiate;
  }

  public boolean isCommitted() {
    return this.state == COMMIT_COMPLETE;
  }

  @Override
  public String toString() {
    return "GlobalTransactionDescriptor[" + stxn + "," + gid + "," + state + "]";
  }

  public TransactionID getClientTransactionID() {
    return stxn.getClientTransactionID();
  }

  @Override
  public int hashCode() {
    return (37 * stxn.hashCode()) + gid.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (!(o instanceof GlobalTransactionDescriptor)) return false;
    if (o == this) return true;
    GlobalTransactionDescriptor c = (GlobalTransactionDescriptor) o;
    return this.stxn.equals(c.stxn) && this.gid.equals(c.gid);
  }

  public ServerTransactionID getServerTransactionID() {
    return stxn;
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return gid;
  }

  public boolean complete() {
    return (state == COMMIT_COMPLETE);
  }

  public void recordLogicalChangeResults(Map<LogicalChangeID, LogicalChangeResult> results) {
    if (changeResults == null) {
      changeResults = new HashMap<LogicalChangeID, LogicalChangeResult>(results.size());
    }
    changeResults.putAll(results);
  }

  public Map<LogicalChangeID, LogicalChangeResult> getApplyResults() {
    return (Map<LogicalChangeID, LogicalChangeResult>) (changeResults == null ? Collections.emptyMap() : changeResults);
  }
}
