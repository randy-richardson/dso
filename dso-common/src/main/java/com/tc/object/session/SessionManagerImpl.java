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
package com.tc.object.session;

import com.tc.net.NodeID;
import com.tc.util.sequence.Sequence;

import java.util.HashMap;
import java.util.Map;

public class SessionManagerImpl implements SessionManager, SessionProvider {
  private final SequenceFactory       sequenceFactory;
  private final Map<NodeID, Provider> providersMap = new HashMap<NodeID, Provider>();

  public SessionManagerImpl(SequenceFactory sequenceFactory) {
    this.sequenceFactory = sequenceFactory;
  }

  @Override
  public boolean isCurrentSession(NodeID nid, SessionID sessionID) {
    Provider provider = getProvider(nid);
    return provider.isCurrentSession(sessionID);
  }

  @Override
  public void newSession(NodeID nid) {
    Provider provider = getProvider(nid);
    provider.newSession();
  }

  @Override
  public SessionID getSessionID(NodeID nid) {
    Provider provider = getProvider(nid);
    return provider.getSessionID();
  }

  @Override
  public SessionID nextSessionID(NodeID nid) {
    Provider provider = getProvider(nid);
    return provider.nextSessionID();
  }

  @Override
  public void initProvider(NodeID nid) {
    synchronized (providersMap) {
      Provider provider = new Provider(sequenceFactory.newSequence());
      if (providersMap.put(nid, provider) != null) { throw new RuntimeException("Session provider already exists for "
                                                                                + nid); }
    }
  }

  @Override
  public void resetSessionProvider() {
    synchronized (providersMap) {
      providersMap.clear();
    }
  }

  private Provider getProvider(NodeID nid) {
    synchronized (providersMap) {
      Provider provider = providersMap.get(nid);
      if (provider == null) { throw new RuntimeException("Session provider does not exist for " + nid); }
      return provider;
    }
  }

  public interface SequenceFactory {
    public Sequence newSequence();
  }

  private static class Provider {
    private final Sequence sequence;
    private SessionID      sessionID     = SessionID.NULL_ID;
    private SessionID      nextSessionID = SessionID.NULL_ID;

    public Provider(Sequence sequence) {
      this.sequence = sequence;
    }

    public synchronized SessionID getSessionID() {
      return sessionID;
    }

    /*
     * Return the next session id will be when call newSession. This advances session id but not apply to messages
     * creation. Message filter uses it to drop old messages when session changes.
     */
    public synchronized SessionID nextSessionID() {
      if (nextSessionID == SessionID.NULL_ID) {
        nextSessionID = new SessionID(sequence.next());
      }
      return nextSessionID;
    }

    public synchronized void newSession() {
      if (nextSessionID != SessionID.NULL_ID) {
        sessionID = nextSessionID;
        nextSessionID = SessionID.NULL_ID;
      } else {
        sessionID = new SessionID(sequence.next());
      }
    }

    public synchronized boolean isCurrentSession(SessionID compare) {
      return sessionID.equals(compare);
    }

    @Override
    public synchronized String toString() {
      return getClass().getName() + "[current session=" + sessionID + "]";
    }
  }

}
