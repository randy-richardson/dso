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
package com.tc.objectserver.tx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.LockID;
import com.tc.object.locks.StringLockID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class TestServerTransaction implements ServerTransaction {

  public List                       changes = new ArrayList();
  public ObjectIDSet                oids    = new BitSetObjectIDSet();
  private final ServerTransactionID sid;
  private final TxnBatchID          bid;
  private final GlobalTransactionID gtid;
  public long[]                     hwm;
  public MetaDataReader[]           metaDataReaders;
  public boolean                    resent = false;

  public TestServerTransaction(ServerTransactionID sid, TxnBatchID bid) {
    this(sid, bid, null);
  }

  public TestServerTransaction(ServerTransactionID sid, TxnBatchID bid, GlobalTransactionID gtid) {
    this.sid = sid;
    this.bid = bid;
    this.gtid = gtid;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    throw new ImplementMe();
  }

  @Override
  public LockID[] getLockIDs() {
    return new LockID[] { new StringLockID("saro") };
  }

  @Override
  public NodeID getSourceID() {
    return this.sid.getSourceID();
  }

  @Override
  public TransactionID getTransactionID() {
    return this.sid.getClientTransactionID();
  }

  @Override
  public SequenceID getClientSequenceID() {
    return SequenceID.NULL_ID;
  }

  @Override
  public List getChanges() {
    return this.changes;
  }

  @Override
  public Map getNewRoots() {
    return Collections.EMPTY_MAP;
  }

  @Override
  public TxnType getTransactionType() {
    throw new ImplementMe();
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return this.oids;
  }

  @Override
  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public ServerTransactionID getServerTransactionID() {
    return this.sid;
  }

  @Override
  public TxnBatchID getBatchID() {
    return this.bid;
  }

  @Override
  public MetaDataReader[] getMetaDataReaders() {
    return this.metaDataReaders;
  }

  public boolean isPassive() {
    return false;
  }

  @Override
  public ObjectIDSet getNewObjectIDs() {
    throw new ImplementMe();
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    if (this.gtid != null) { return this.gtid; }
    throw new AssertionError("Gid is not set !");
  }

  @Override
  public boolean isActiveTxn() {
    return true;
  }

  @Override
  public boolean isResent() {
    return resent;
  }

  @Override
  public int getNumApplicationTxn() {
    return 1;
  }

  @Override
  public void setGlobalTransactionID(GlobalTransactionID gid) {
    throw new ImplementMe();
  }

  @Override
  public long[] getHighWaterMarks() {
    return this.hwm;
  }

  @Override
  public boolean isSearchEnabled() {
    return false;
  }

  @Override
  public boolean isEviction() {
    return false;
  }
}