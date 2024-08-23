/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.tc.net.NodeID;
import com.tc.object.dna.api.MetaDataReader;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.locks.LockID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.util.SequenceID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PassiveServerTransactionImpl extends ServerTransactionImpl {

  public PassiveServerTransactionImpl(TxnBatchID batchID, TransactionID txID, SequenceID sequenceID, LockID[] lockIDs,
                                      NodeID source, List dnas, ObjectStringSerializer serializer, Map newRoots,
                                      TxnType transactionType, Collection notifies,
                                      MetaDataReader [] metaDataReaders, int numApplicationTxn, long[] highWaterMarks) {
    super(batchID, txID, sequenceID, lockIDs, source, dnas, serializer, newRoots, transactionType, notifies,
          metaDataReaders, numApplicationTxn, highWaterMarks);
  }

  @Override
  public Collection getNotifies() {
    return Collections.EMPTY_LIST;
  }

  @Override
  public ObjectStringSerializer getSerializer() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isActiveTxn() {
    return false;
  }

  @Override
  public String toString() {
    return "PassiveServerTransactionImpl [ " + super.toString() + " ]";
  }
}
