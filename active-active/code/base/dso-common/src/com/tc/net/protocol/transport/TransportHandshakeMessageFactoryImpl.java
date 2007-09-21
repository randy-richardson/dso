/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.exception.TCInternalError;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.core.TCConnection;
import com.tc.net.protocol.TCProtocolException;

public class TransportHandshakeMessageFactoryImpl implements TransportHandshakeMessageFactory {
  public static final ConnectionID DEFAULT_ID = ConnectionID.NULL_ID;

  public TransportHandshakeMessage createSyn(ConnectionID connectionId, TCConnection source, boolean newConnect) {
    return createNewMessage(TransportHandshakeMessageImpl.SYN, connectionId, null, source, false, 0, newConnect);
  }


  public TransportHandshakeMessage createAck(ConnectionID connectionId, TCConnection source) {
    return createNewMessage(TransportHandshakeMessageImpl.ACK, connectionId, null, source, false, 0, false);
  }

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TCConnection source,
                                                boolean isMaxConnectionsExceeded, int maxConnections) {
    return createSynAck(connectionId, null, source, isMaxConnectionsExceeded, maxConnections);
  }

  public TransportHandshakeMessage createSynAck(ConnectionID connectionId, TransportHandshakeErrorContext errorContext,
                                                TCConnection source, boolean isMaxConnectionsExceeded,
                                                int maxConnections) {
    return createNewMessage(TransportHandshakeMessageImpl.SYN_ACK, connectionId, errorContext, source,
                            isMaxConnectionsExceeded, maxConnections, false);
  }

  private TransportHandshakeMessage createNewMessage(byte type, ConnectionID connectionId,
                                                     TransportHandshakeErrorContext errorContext, TCConnection source,
                                                     boolean isMaxConnectionsExceeded, int maxConnections, boolean newConnect) {
    TCByteBufferOutputStream bbos = new TCByteBufferOutputStream();

    bbos.write(TransportHandshakeMessageImpl.VERSION_1);
    bbos.write(type);
    bbos.writeBoolean(newConnect);  // indicate using ChannelMultiplex
    bbos.writeString(connectionId.getID());
    bbos.writeBoolean(isMaxConnectionsExceeded);
    bbos.writeInt(maxConnections);
    bbos.writeBoolean(errorContext != null);
    if (errorContext != null) bbos.writeString(errorContext.toString());

    final WireProtocolHeader header = new WireProtocolHeader();
    header.setProtocol(WireProtocolHeader.PROTOCOL_TRANSPORT_HANDSHAKE);

    final TransportHandshakeMessageImpl message;
    try {
      message = new TransportHandshakeMessageImpl(source, header, bbos.toArray());
    } catch (TCProtocolException e) {
      throw new TCInternalError(e);
    }
    return message;
  }
}