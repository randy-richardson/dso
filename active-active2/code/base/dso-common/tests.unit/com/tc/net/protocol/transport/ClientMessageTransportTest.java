/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.MockConnectionManager;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.event.TCConnectionEvent;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.ClientMessageChannelMultiplex;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageSink;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.test.TCTestCase;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * x normal connect and handshake o reconnect and handshake
 */
public class ClientMessageTransportTest extends TCTestCase {
  private ConnectionID                       connectionId;
  private ClientMessageTransport             transport;
  private MockConnectionManager              connectionManager;
  private MockTCConnection                   connection;
  private TransportHandshakeMessageFactory   transportMessageFactory;
  private TestTransportHandshakeErrorHandler handshakeErrorHandler;
  private final int                          maxRetries = 10;
  private MessageTransportFactory            transportFactory;
  private final int                          timeout    = 3000;

  public void setUp() {
    DefaultConnectionIdFactory connectionIDProvider = new DefaultConnectionIdFactory();
    this.connectionId = connectionIDProvider.nextConnectionId();
    this.connectionManager = new MockConnectionManager();
    this.connection = new MockTCConnection();
    this.connectionManager.setConnection(connection);
    this.transportMessageFactory = new TransportMessageFactoryImpl();
    handshakeErrorHandler = new TestTransportHandshakeErrorHandler();

    final ConnectionInfo connectionInfo = new ConnectionInfo("", 0);
    ClientConnectionEstablisher cce = new ClientConnectionEstablisher(
                                                                      connectionManager,
                                                                      new ConnectionAddressProvider(
                                                                                                    new ConnectionInfo[] { connectionInfo }),
                                                                      maxRetries, 5000);
    transport = new ClientMessageTransport(cce, handshakeErrorHandler, this.transportMessageFactory,
                                           new WireProtocolAdaptorFactoryImpl(),
                                           TransportHandshakeMessage.NO_CALLBACK_PORT, new TestClientMessageChannel());
  }

  public void testRoundRobinReconnect() throws Exception {
    SynchronizedRef errorRef = new SynchronizedRef(null);
    ClientHandshakeMessageResponder tester = new ClientHandshakeMessageResponder(new LinkedQueue(), new LinkedQueue(),
                                                                                 this.transportMessageFactory,
                                                                                 this.connectionId, this.transport,
                                                                                 errorRef);
    this.connection.setMessageSink(tester);

    transport.open();
    while (!connection.connectCalls.isEmpty()) {
      connection.connectCalls.take();
    }

    connection.fail = true;
    transport.closeEvent(new TCConnectionEvent(connection));

    // FIXME 2005-12-14 -- We should restore this test.
    // assertNull(connection.connectCalls.poll(3000));

  }

  public void testConnectAndHandshake() throws Exception {
    SynchronizedRef errorRef = new SynchronizedRef(null);
    ClientHandshakeMessageResponder tester = new ClientHandshakeMessageResponder(new LinkedQueue(), new LinkedQueue(),
                                                                                 this.transportMessageFactory,
                                                                                 this.connectionId, this.transport,
                                                                                 errorRef);

    this.connection.setMessageSink(tester);

    transport.open();

    assertTrue(errorRef.get() == null);

    List sentMessages = connection.getSentMessages();

    assertEquals(2, sentMessages.size());
    assertEquals(this.connectionId, transport.getConnectionId());
    Thread.sleep(1000);
    assertTrue(tester.waitForAckToBeReceived(timeout));
  }

  /**
   * Test interaction with a real network listener.
   */
  public void testConnectAndHandshakeActuallyConnected() throws Exception {
    CommunicationsManager commsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                   new TransportNetworkStackHarnessFactory(),
                                                                   new NullConnectionPolicy(), 0);
    NetworkListener listener = commsMgr.createListener(new NullSessionManager(), new TCSocketAddress(0), true,
                                                       new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    int port = listener.getBindPort();

    final ConnectionInfo connInfo = new ConnectionInfo(TCSocketAddress.LOOPBACK_IP, port);
    ClientConnectionEstablisher cce = new ClientConnectionEstablisher(
                                                                      commsMgr.getConnectionManager(),
                                                                      new ConnectionAddressProvider(
                                                                                                    new ConnectionInfo[] { connInfo }),
                                                                      0, 1000);
    transport = new ClientMessageTransport(cce, this.handshakeErrorHandler, this.transportMessageFactory,
                                           new WireProtocolAdaptorFactoryImpl(),
                                           TransportHandshakeMessage.NO_CALLBACK_PORT, new TestClientMessageChannel());
    transport.open();
    assertTrue(transport.isConnected());
    listener.stop(5000);

  }

  /**
   * This test is for testing the communication stack layer mismatch between the server and L1s while handshaking
   */
  public void testStackLayerMismatch() throws Exception {
    // Case 1: Server has the OOO layer and client doesn't

    CommunicationsManager serverCommsMgr = new CommunicationsManagerImpl(
                                                                         new NullMessageMonitor(),
                                                                         new OOONetworkStackHarnessFactory(
                                                                                                           new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                                           null,
                                                                                                           new L1ReconnectConfigImpl()),
                                                                         new NullConnectionPolicy(), 0);

    CommunicationsManager clientCommsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                                         new PlainNetworkStackHarnessFactory(),
                                                                         new NullConnectionPolicy(), 0);

    try {
      createStacksAndTest(serverCommsMgr, clientCommsMgr);
    } finally {
      try {
        clientCommsMgr.shutdown();
      } finally {
        serverCommsMgr.shutdown();
      }
    }

    // Case 2: Client has the OOO layer and server doesn't
    serverCommsMgr = new CommunicationsManagerImpl(new NullMessageMonitor(), new PlainNetworkStackHarnessFactory(),
                                                   new NullConnectionPolicy(), 0);

    clientCommsMgr = new CommunicationsManagerImpl(
                                                   new NullMessageMonitor(),
                                                   new OOONetworkStackHarnessFactory(
                                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                                     null, new L1ReconnectConfigImpl()),
                                                   new NullConnectionPolicy(), 0);

    try {
      createStacksAndTest(serverCommsMgr, clientCommsMgr);
    } finally {
      try {
        clientCommsMgr.shutdown();
      } finally {
        serverCommsMgr.shutdown();
      }
    }

  }

  private void createStacksAndTest(final CommunicationsManager serverCommsMgr,
                                   final CommunicationsManager clientCommsMgr) throws Exception {
    NetworkListener listener = serverCommsMgr.createListener(new NullSessionManager(),
                                                             new TCSocketAddress(TCSocketAddress.LOOPBACK_IP, 0), true,
                                                             new DefaultConnectionIdFactory());
    listener.start(Collections.EMPTY_SET);
    final int port = listener.getBindPort();

    // set up the transport factory
    transportFactory = new MessageTransportFactory() {
      public MessageTransport createNewTransport() {
        ClientConnectionEstablisher clientConnectionEstablisher = new ClientConnectionEstablisher(
                                                                                                  serverCommsMgr
                                                                                                      .getConnectionManager(),
                                                                                                  new ConnectionAddressProvider(
                                                                                                                                new ConnectionInfo[] { new ConnectionInfo(
                                                                                                                                                                          "localhost",
                                                                                                                                                                          port) }),
                                                                                                  maxRetries, timeout);
        ClientMessageTransport cmt = new ClientMessageTransport(clientConnectionEstablisher, handshakeErrorHandler,
                                                                transportMessageFactory,
                                                                new WireProtocolAdaptorFactoryImpl(),
                                                                TransportHandshakeMessage.NO_CALLBACK_PORT, new TestClientMessageChannel());
        return cmt;
      }

      public MessageTransport createNewTransport(ConnectionID connectionID, TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        throw new AssertionError();
      }

      public MessageTransport createNewTransport(ConnectionID connectionID, TCConnection tcConnection,
                                                 TransportHandshakeErrorHandler handler,
                                                 TransportHandshakeMessageFactory handshakeMessageFactory,
                                                 List transportListeners) {
        throw new AssertionError();
      }
    };

    ClientMessageChannel channel;
    channel = clientCommsMgr
        .createClientChannel(
                             new NullSessionManager(),
                             0,
                             TCSocketAddress.LOOPBACK_IP,
                             port,
                             timeout,
                             new ConnectionAddressProvider(
                                                           new ConnectionInfo[] { new ConnectionInfo("localhost", port) }),
                             transportFactory);
    try {
      channel.open();
    } catch (TCTimeoutException e) {
      // this is an expected timeout exception as the client will get an handshake error and it is not killing itself
      // do nothing
    }
    assertTrue(handshakeErrorHandler.getStackLayerMismatch());
    listener.stop(5000);
  }

  private static class TestTransportHandshakeErrorHandler implements TransportHandshakeErrorHandler {

    private boolean stackLayerMismatch = false;

    public void handleHandshakeError(TransportHandshakeErrorContext e) {
      if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) stackLayerMismatch = true;
    }

    public void handleHandshakeError(TransportHandshakeErrorContext e, TransportHandshakeMessage m) {
      if (e.getErrorType() == TransportHandshakeError.ERROR_STACK_MISMATCH) stackLayerMismatch = true;
    }

    public boolean getStackLayerMismatch() {
      return stackLayerMismatch;
    }
  }
  
  private class TestClientMessageChannel implements ClientMessageChannel {
    private final TCMessageFactory              msgFactory;
    private int                                 connectAttemptCount;
    private int                                 connectCount;
    private ChannelID                           channelID;
    private final SessionProvider               sessionProvider;
    private SessionID                           channelSessionID = SessionID.NULL_ID;
    private final ClientMessageChannelMultiplex multiplex;
    private final ConnectionAddressProvider     addrProvider;
    private final boolean                       activeCoordinator;
    private boolean                             initConnect      = true;
    private NodeID                              source;
    private NodeID                              destination;

    public TestClientMessageChannel() {
      this(null, null, null, null, null, true);
    }
    
    public TestClientMessageChannel(TCMessageFactory msgFactory, TCMessageRouter router,
                                       SessionProvider sessionProvider, ConnectionAddressProvider addrProvider,
                                       ClientMessageChannelMultiplex multiplex, boolean activeCoordinator) {
      this.msgFactory = msgFactory;
      this.sessionProvider = sessionProvider;
      this.multiplex = multiplex;
      this.activeCoordinator = activeCoordinator;
      this.addrProvider = addrProvider;

      setClientID(ClientID.NULL_ID);
      // XXX setServerID(GroupID.NULL_ID);
    }

    public boolean isActiveCoordinator() {
      return activeCoordinator;
    }
    
    public ClientMessageChannel getActiveCoordinator() {
      return this;
    }
    
    public boolean isInitConnect() {
      return initConnect;
    }

    public void connected() {
      initConnect = false;
    }

    public void addClassMapping(TCMessageType type, Class msgClass) {
      throw new ImplementMe();
      
    }

    public ChannelID getActiveActiveChannelID() {
      return channelID;
    }

    public ChannelIDProvider getChannelIDProvider() {
      throw new ImplementMe();
    }

    public int getConnectAttemptCount() {
      throw new ImplementMe();
    }

    public int getConnectCount() {
      throw new ImplementMe();
    }

    public ClientMessageChannelMultiplex getMultiplex() {
      throw new ImplementMe();
    }

    public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
      throw new ImplementMe();
      
    }

    public void routeMessageType(TCMessageType type, TCMessageSink sink) {
      throw new ImplementMe();
      
    }

    public void unrouteMessageType(TCMessageType type) {
      throw new ImplementMe();
      
    }

    public void addAttachment(String key, Object value, boolean replace) {
      throw new ImplementMe();
      
    }

    public void addListener(ChannelEventListener listener) {
      throw new ImplementMe();
      
    }

    public void close() {
      throw new ImplementMe();
      
    }

    public TCMessage createMessage(TCMessageType type) {
      throw new ImplementMe();
    }

    public Object getAttachment(String key) {
      throw new ImplementMe();
    }

    public ChannelID getChannelID() {
      throw new ImplementMe();
    }

    public NodeID getServerID() {
      throw new ImplementMe();
    }

    public TCSocketAddress getLocalAddress() {
      throw new ImplementMe();
    }

    public TCSocketAddress getRemoteAddress() {
      throw new ImplementMe();
    }

    public NodeID getClientID() {
      throw new ImplementMe();
    }

    public boolean isClosed() {
      throw new ImplementMe();
    }

    public boolean isConnected() {
      throw new ImplementMe();
    }

    public boolean isOpen() {
      throw new ImplementMe();
    }

    public NetworkStackID open() throws MaxConnectionsExceededException, TCTimeoutException, UnknownHostException, IOException {
      throw new ImplementMe();
    }

    public Object removeAttachment(String key) {
      throw new ImplementMe();
    }

    public void send(TCNetworkMessage message) {
      throw new ImplementMe();
      
    }

    public void setServerID(NodeID destination) {
      this.destination = destination;
    }

    public void setClientID(NodeID source) {
      this.source = source;
    }

    public ConnectionAddressProvider getConnectionAddress() {
      throw new ImplementMe();
    }

    public TCMessage createMessage(NodeID sendToNode, TCMessageType type) {
      throw new ImplementMe();
    }

    public NodeID getDestinationNodeID() {
      throw new ImplementMe();
    }

    public NodeID getSourceNodeID() {
      throw new ImplementMe();
    }

    public void setDestinationNodeID(NodeID destination) {
      throw new ImplementMe();
    }

    public void setSourceNodeID(NodeID source) {
      throw new ImplementMe();
    }
  }
}
